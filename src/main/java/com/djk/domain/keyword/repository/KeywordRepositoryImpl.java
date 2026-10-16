package com.djk.domain.keyword.repository;

import static com.djk.domain.keyword.entity.QKeyword.keyword;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.keyword.dto.ScoreRange;
import com.djk.domain.category.entity.QCategory;
import com.djk.domain.keyword.entity.QKeyword;
import com.djk.domain.keyword.entity.QKeywordCategory;
import com.djk.domain.keyword.entity.QKeywordRelation;
import com.djk.domain.keyword.entity.QTrendIndexHistory;
import com.djk.domain.keyword.entity.TrendIndexHistory;
import com.djk.domain.ranking.dto.SnapshotTimes;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.domain.ranking.entity.QRankingSnapshot;
import com.djk.domain.ranking.entity.QRankingSnapshotBadge;
import com.djk.domain.ranking.entity.RankingType;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class KeywordRepositoryImpl implements KeywordRepositoryCustom {

    private static final QRankingSnapshot trend = new QRankingSnapshot("trend");
    private static final QRankingSnapshot potential = new QRankingSnapshot("potential");
    private static final QRankingSnapshot rising = new QRankingSnapshot("rising");

    private final JPAQueryFactory queryFactory;

    @Override
    public List<KeywordSearchRow> search(KeywordSearchCondition condition, SnapshotTimes latest) {
        return joinLatestSnapshots(queryFactory.select(keyword, trend, potential, rising), latest)
                .where(searchConditions(condition))
                .orderBy(trend.trendScore.desc().nullsLast(), keyword.id.asc())
                .offset(condition.offset())
                .limit(condition.size())
                .fetch()
                .stream()
                .map(row -> new KeywordSearchRow(
                        row.get(keyword), row.get(trend), row.get(potential), row.get(rising)))
                .toList();
    }

    @Override
    public long countSearch(KeywordSearchCondition condition, SnapshotTimes latest) {
        Long count = joinLatestSnapshots(queryFactory.select(keyword.count()), latest)
                .where(searchConditions(condition))
                .fetchOne();
        return count == null ? 0 : count;
    }

    @Override
    public Map<Long, List<String>> findCategoryNames(Collection<Long> keywordIds) {
        QKeywordCategory keywordCategory = QKeywordCategory.keywordCategory;
        QCategory category = QCategory.category;
        Map<Long, List<String>> namesByKeywordId = new HashMap<>();
        queryFactory
                .select(keywordCategory.keyword.id, category.name)
                .from(keywordCategory)
                .join(keywordCategory.category, category)
                .where(keywordCategory.keyword.id.in(keywordIds))
                .orderBy(category.id.asc())
                .fetch()
                .forEach(row -> namesByKeywordId
                        .computeIfAbsent(row.get(keywordCategory.keyword.id), id -> new ArrayList<>())
                        .add(row.get(category.name)));
        return namesByKeywordId;
    }

    /**
     * 키워드별 상위 limit 개를 SQL 에서 자르지 않고(JPQL 에 윈도 함수가 없다) 연관도순으로 모두 읽은 뒤 앞에서 자른다.
     * idx_relation_keyword_score 인덱스를 탄다.
     */
    @Override
    public Map<Long, List<String>> findTopRelatedKeywordNames(Collection<Long> keywordIds, int limit) {
        QKeywordRelation relation = QKeywordRelation.keywordRelation;
        QKeyword related = new QKeyword("related");
        Map<Long, List<String>> namesByKeywordId = new HashMap<>();
        queryFactory
                .select(relation.keyword.id, related.keywordName)
                .from(relation)
                .join(relation.relatedKeyword, related)
                .where(relation.keyword.id.in(keywordIds))
                .orderBy(relation.keyword.id.asc(), relation.relevanceScore.desc(), related.id.asc())
                .fetch()
                .forEach(row -> {
                    List<String> names = namesByKeywordId
                            .computeIfAbsent(row.get(relation.keyword.id), id -> new ArrayList<>());
                    if (names.size() < limit) {
                        names.add(row.get(related.keywordName));
                    }
                });
        return namesByKeywordId;
    }

    @Override
    public Map<Long, List<TrendIndexHistory>> findTrendHistories(Collection<Long> keywordIds, LocalDateTime from,
                                                                 LocalDateTime to) {
        QTrendIndexHistory history = QTrendIndexHistory.trendIndexHistory;
        return queryFactory
                .selectFrom(history)
                .where(history.keyword.id.in(keywordIds), history.measuredAt.between(from, to))
                .orderBy(history.measuredAt.asc())
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(point -> point.getKeyword().getId()));
    }

    /** 키워드마다 세 랭킹 유형의 최신 스냅샷 row 를 붙인다. 유형별로 키워드당 row 는 최대 하나다. */
    private <T> JPAQuery<T> joinLatestSnapshots(JPAQuery<T> query, SnapshotTimes latest) {
        return query.from(keyword)
                .leftJoin(trend).on(isLatestRow(trend, RankingType.TREND, latest))
                .leftJoin(potential).on(isLatestRow(potential, RankingType.POTENTIAL, latest))
                .leftJoin(rising).on(isLatestRow(rising, RankingType.RISING, latest));
    }

    private BooleanExpression isLatestRow(QRankingSnapshot snapshot, RankingType rankingType,
                                          SnapshotTimes latest) {
        LocalDateTime snapshotAt = latest.of(rankingType);
        // 그 유형의 스냅샷이 없으면 snapshot_at(NOT NULL) IS NULL 로 아무 row 도 붙지 않게 한다.
        BooleanExpression atLatest = snapshotAt == null
                ? snapshot.snapshotAt.isNull()
                : snapshot.snapshotAt.eq(snapshotAt);
        return snapshot.keyword.eq(keyword).and(snapshot.rankingType.eq(rankingType)).and(atLatest);
    }

    /** null 인 조건은 QueryDSL where 가 무시한다. */
    private Predicate[] searchConditions(KeywordSearchCondition condition) {
        return new Predicate[] {
                trend.id.isNotNull().or(potential.id.isNotNull()).or(rising.id.isNotNull()),
                matchesQuery(condition.query()),
                inCategory(condition.categoryId()),
                hasAnyBadge(condition.badgeTypes()),
                scoreWithin(trend, condition.trendRange()),
                scoreWithin(potential, condition.potentialRange()),
                condition.risingOnly() ? rising.id.isNotNull() : null
        };
    }

    /** 범위를 좁혔을 때만 건다. 그 랭킹에 없는 키워드(점수 null)는 between 에서 빠진다. */
    private BooleanExpression scoreWithin(QRankingSnapshot snapshot, ScoreRange range) {
        if (range.isUnbounded()) {
            return null;
        }
        return snapshot.trendScore.between(range.lowerScore(), range.upperScore());
    }

    /** 최신 TREND·POTENTIAL·RISING 스냅샷 row 중 하나에 그 뱃지가 붙어 있다. */
    private BooleanExpression hasAnyBadge(List<BadgeType> badgeTypes) {
        if (badgeTypes.isEmpty()) {
            return null;
        }
        QRankingSnapshotBadge badge = QRankingSnapshotBadge.rankingSnapshotBadge;
        return JPAExpressions.selectOne()
                .from(badge)
                .where(badge.rankingSnapshot.eq(trend)
                                .or(badge.rankingSnapshot.eq(potential))
                                .or(badge.rankingSnapshot.eq(rising)),
                        badge.badgeType.in(badgeTypes))
                .exists();
    }

    private BooleanExpression inCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        QKeywordCategory keywordCategory = QKeywordCategory.keywordCategory;
        return keyword.id.in(JPAExpressions
                .select(keywordCategory.keyword.id)
                .from(keywordCategory)
                .where(keywordCategory.category.id.eq(categoryId)));
    }

    /** 이름에 검색어가 들어간 키워드, 또는 이름이 검색어와 같은 키워드의 연관어. 대소문자는 무시한다. */
    private BooleanExpression matchesQuery(String query) {
        if (query == null) {
            return null;
        }
        QKeywordRelation relation = QKeywordRelation.keywordRelation;
        return keyword.keywordName.containsIgnoreCase(query)
                .or(keyword.id.in(JPAExpressions
                        .select(relation.relatedKeyword.id)
                        .from(relation)
                        .where(relation.keyword.keywordName.equalsIgnoreCase(query))));
    }
}
