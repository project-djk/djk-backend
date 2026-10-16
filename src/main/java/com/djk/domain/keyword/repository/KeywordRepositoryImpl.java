package com.djk.domain.keyword.repository;

import static com.djk.domain.keyword.entity.QKeyword.keyword;

import com.djk.domain.keyword.dto.KeywordSearchCondition;
import com.djk.domain.keyword.dto.KeywordSearchRow;
import com.djk.domain.keyword.dto.ScoreRange;
import com.djk.domain.keyword.entity.QKeywordCategory;
import com.djk.domain.keyword.entity.QKeywordRelation;
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
import java.util.List;
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
