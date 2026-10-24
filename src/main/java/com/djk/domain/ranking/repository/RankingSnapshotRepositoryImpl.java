package com.djk.domain.ranking.repository;

import static com.djk.domain.keyword.entity.QKeyword.keyword;
import static com.djk.domain.ranking.entity.QRankingSnapshot.rankingSnapshot;

import com.djk.domain.ranking.dto.SnapshotTimes;
import com.djk.domain.ranking.entity.BadgeType;
import com.djk.domain.ranking.entity.QRankingSnapshot;
import com.djk.domain.ranking.entity.QRankingSnapshotBadge;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class RankingSnapshotRepositoryImpl implements RankingSnapshotRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RankingSnapshot> findLatestSnapshotRows(RankingType rankingType, int limit) {
        QRankingSnapshot latest = new QRankingSnapshot("latest");
        return queryFactory
                .selectFrom(rankingSnapshot)
                .join(rankingSnapshot.keyword, keyword).fetchJoin()
                .where(rankingSnapshot.rankingType.eq(rankingType),
                        rankingSnapshot.snapshotAt.eq(JPAExpressions
                                .select(latest.snapshotAt.max())
                                .from(latest)
                                .where(latest.rankingType.eq(rankingType))))
                .orderBy(rankingSnapshot.rankPosition.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public SnapshotTimes findLatestSnapshotTimes() {
        List<Tuple> rows = queryFactory
                .select(rankingSnapshot.rankingType, rankingSnapshot.snapshotAt.max())
                .from(rankingSnapshot)
                .groupBy(rankingSnapshot.rankingType)
                .fetch();
        Map<RankingType, LocalDateTime> byType = new EnumMap<>(RankingType.class);
        for (Tuple row : rows) {
            byType.put(row.get(rankingSnapshot.rankingType), row.get(rankingSnapshot.snapshotAt.max()));
        }
        return new SnapshotTimes(byType);
    }

    @Override
    public LocalDateTime findPreviousSnapshotTime(RankingType rankingType, LocalDateTime before) {
        return queryFactory
                .select(rankingSnapshot.snapshotAt.max())
                .from(rankingSnapshot)
                .where(rankingSnapshot.rankingType.eq(rankingType), rankingSnapshot.snapshotAt.lt(before))
                .fetchOne();
    }

    @Override
    public Map<Long, Integer> findRankPositions(RankingType rankingType, LocalDateTime snapshotAt,
                                                Collection<Long> keywordIds) {
        Map<Long, Integer> rankByKeywordId = new HashMap<>();
        queryFactory
                .select(rankingSnapshot.keyword.id, rankingSnapshot.rankPosition)
                .from(rankingSnapshot)
                .where(rankingSnapshot.rankingType.eq(rankingType),
                        rankingSnapshot.snapshotAt.eq(snapshotAt),
                        rankingSnapshot.keyword.id.in(keywordIds))
                .fetch()
                .forEach(row -> rankByKeywordId.put(
                        row.get(rankingSnapshot.keyword.id), row.get(rankingSnapshot.rankPosition)));
        return rankByKeywordId;
    }

    @Override
    public RankingSnapshot findSnapshotRow(RankingType rankingType, LocalDateTime snapshotAt, long keywordId) {
        return queryFactory
                .selectFrom(rankingSnapshot)
                .where(rankingSnapshot.rankingType.eq(rankingType),
                        rankingSnapshot.snapshotAt.eq(snapshotAt),
                        rankingSnapshot.keyword.id.eq(keywordId))
                .fetchOne();
    }

    @Override
    public Map<Long, Set<BadgeType>> findBadgeTypesBySnapshotIds(Collection<Long> rankingSnapshotIds) {
        QRankingSnapshotBadge badge = QRankingSnapshotBadge.rankingSnapshotBadge;
        Map<Long, Set<BadgeType>> badgesByKeywordId = new HashMap<>();
        queryFactory
                .select(badge.rankingSnapshot.keyword.id, badge.badgeType)
                .from(badge)
                .where(badge.rankingSnapshot.id.in(rankingSnapshotIds))
                .fetch()
                .forEach(row -> badgesByKeywordId
                        .computeIfAbsent(row.get(badge.rankingSnapshot.keyword.id),
                                id -> EnumSet.noneOf(BadgeType.class))
                        .add(row.get(badge.badgeType)));
        return badgesByKeywordId;
    }
}
