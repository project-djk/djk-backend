package com.djk.domain.ranking.repository;

import static com.djk.domain.keyword.entity.QKeyword.keyword;
import static com.djk.domain.ranking.entity.QRankingSnapshot.rankingSnapshot;

import com.djk.domain.ranking.entity.QRankingSnapshot;
import com.djk.domain.ranking.entity.RankingSnapshot;
import com.djk.domain.ranking.entity.RankingType;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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
}
