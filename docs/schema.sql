-- ============================================================
-- DJK 트렌드 분석기 - DB 스키마 최종본 (MySQL 8.0.16+)
-- 담당: 김송은
-- 확정 사항
--   * 집계 주기: 일단 1일 1회 (성능 이슈 시 추후 변경)
--   * trend_score: 0~1 정규화 값 DECIMAL(6,4) (화면 표시 시 x100)
--   * 뱃지: ENUM + RANKING_SNAPSHOT_BADGE (랭킹 스냅샷에 종속)
--   * 테이블명: TB_ 접두사 없이 통일
-- ============================================================

CREATE DATABASE IF NOT EXISTS djk
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE djk;

-- ------------------------------------------------------------
-- USERS : 사용자
-- ------------------------------------------------------------
CREATE TABLE USERS (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    email          VARCHAR(255) NOT NULL COMMENT '이메일',
    password       VARCHAR(255) NOT NULL COMMENT '비밀번호 (해시값 저장)',
    nickname       VARCHAR(30)  NOT NULL COMMENT '닉네임',
    is_onboarded   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '온보딩 완료 여부',
    last_login_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '마지막 로그인 일시',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    modified_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB COMMENT='사용자 정보';

-- ------------------------------------------------------------
-- CATEGORY : 카테고리 (개수/종류는 팀 논의 후 초기 데이터 확정)
-- ------------------------------------------------------------
CREATE TABLE CATEGORY (
    id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    name  VARCHAR(255) NOT NULL COMMENT '카테고리 이름',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB COMMENT='카테고리';

-- ------------------------------------------------------------
-- KEYWORD : 키워드 마스터
-- ------------------------------------------------------------
CREATE TABLE KEYWORD (
    id                          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '키워드 고유 ID',
    keyword_name                VARCHAR(100) NOT NULL COMMENT '키워드명',
    first_collected_at          DATETIME     NOT NULL COMMENT '최초 수집 일시',
    trend_last_ranked_out_at    DATETIME     NULL DEFAULT NULL COMMENT 'trend ranking 순위권 이탈 일자 (재진입 정보 표시용)',
    potential_last_ranked_out_at DATETIME    NULL DEFAULT NULL COMMENT 'potential ranking 순위권 이탈 일자 (재진입 정보 표시용)',
    current_rank_entered_at     DATETIME     NULL DEFAULT NULL COMMENT 'steady 계산용. 트렌드 랭킹 순위권에 들어온 일시, 순위권 밖이면 NULL',
    updated_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 업데이트 일시',
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword_name (keyword_name)
) ENGINE=InnoDB COMMENT='서비스가 수집·추적하는 키워드';

-- ------------------------------------------------------------
-- KEYWORD_CATEGORY : 키워드-카테고리 N:M
-- ------------------------------------------------------------
CREATE TABLE KEYWORD_CATEGORY (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    keyword_id   BIGINT NOT NULL COMMENT '키워드 ID',
    category_id  BIGINT NOT NULL COMMENT '카테고리 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword_category (keyword_id, category_id),
    KEY idx_keyword_category_category (category_id),
    CONSTRAINT fk_kc_keyword  FOREIGN KEY (keyword_id)  REFERENCES KEYWORD (id)  ON DELETE CASCADE,
    CONSTRAINT fk_kc_category FOREIGN KEY (category_id) REFERENCES CATEGORY (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='키워드-카테고리 다대다 (한 키워드가 여러 카테고리에 속할 수 있음)';

-- ------------------------------------------------------------
-- USER_CATEGORY : 유저-카테고리 N:M
-- ------------------------------------------------------------
CREATE TABLE USER_CATEGORY (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '고유 ID',
    user_id      BIGINT NOT NULL COMMENT '유저 ID',
    category_id  BIGINT NOT NULL COMMENT '카테고리 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_category (user_id, category_id),
    KEY idx_user_category_category (category_id),
    CONSTRAINT fk_uc_user     FOREIGN KEY (user_id)     REFERENCES USERS (id)    ON DELETE CASCADE,
    CONSTRAINT fk_uc_category FOREIGN KEY (category_id) REFERENCES CATEGORY (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='유저-카테고리 다대다 (온보딩 관심 카테고리)';

-- ------------------------------------------------------------
-- TREND_INDEX_HISTORY : 키워드별 트렌드 지수 시계열 (그래프/예측용)
-- 화면: 6 키워드 상세 페이지(트렌드 지수 그래프)
-- ------------------------------------------------------------
CREATE TABLE TREND_INDEX_HISTORY (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '이력 고유 ID',
    keyword_id   BIGINT        NOT NULL COMMENT '키워드 ID',
    measured_at  DATETIME      NOT NULL COMMENT '트렌드 지수 측정(수집) 일시',
    trend_score  DECIMAL(6,4)  NOT NULL COMMENT '트렌드 지수 (0~1 정규화, 화면 표시 시 100점 환산)',
    change_rate  DECIMAL(6,2)  NULL DEFAULT NULL COMMENT '직전 측정 대비 변화량(%)',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trend_history_keyword_measured (keyword_id, measured_at),
    CONSTRAINT fk_th_keyword FOREIGN KEY (keyword_id) REFERENCES KEYWORD (id) ON DELETE CASCADE,
    CONSTRAINT ck_th_score CHECK (trend_score BETWEEN 0 AND 1)
) ENGINE=InnoDB COMMENT='키워드 트렌드 지수 히스토리';

-- ------------------------------------------------------------
-- RANKING_SNAPSHOT : 랭킹 스냅샷 (집계 1회당 유형별 TOP100)
-- 화면: 1 추천 검색어, 2 트렌드 랭킹, 3 Rising Trend, 4 Potential Trend
-- 메인 추천 검색어 = ranking_type='TREND' 의 최신 snapshot_at TOP10
-- 재진입 판단 = 직전 snapshot 에 있었는지 비교
-- ------------------------------------------------------------
CREATE TABLE RANKING_SNAPSHOT (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '랭킹 고유 ID',
    ranking_type   ENUM('TREND','RISING','POTENTIAL') NOT NULL COMMENT '랭킹 유형 (TREND / RISING / POTENTIAL)',
    keyword_id     BIGINT        NOT NULL COMMENT '키워드 ID',
    rank_position  INT           NOT NULL COMMENT '랭킹 순위 (1~100)',
    trend_score    DECIMAL(6,4)  NOT NULL COMMENT '스냅샷 시점 트렌드 지수 (0~1)',
    change_amount  DECIMAL(6,2)  NULL DEFAULT NULL COMMENT '지수/순위 변화량 (Rising 산정 및 급상승 뱃지 판정 기준)',
    snapshot_at    DATETIME      NOT NULL COMMENT '집계(iteration) 시점. 한 번 집계된 row 전체가 같은 값 (현재 1일 1회)',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'DB에 row가 실제로 INSERT된 시각 (디버깅용)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ranking_type_snapshot_keyword (ranking_type, snapshot_at, keyword_id),
    KEY idx_ranking_type_snapshot_rank (ranking_type, snapshot_at, rank_position),
    KEY idx_ranking_keyword_snapshot (keyword_id, snapshot_at),
    CONSTRAINT fk_rs_keyword FOREIGN KEY (keyword_id) REFERENCES KEYWORD (id) ON DELETE CASCADE,
    CONSTRAINT ck_rs_rank  CHECK (rank_position BETWEEN 1 AND 100),
    CONSTRAINT ck_rs_score CHECK (trend_score BETWEEN 0 AND 1)
) ENGINE=InnoDB COMMENT='트렌드/Rising/Potential 랭킹 집계 결과';

-- ------------------------------------------------------------
-- RANKING_SNAPSHOT_BADGE : 랭킹 스냅샷에 부여된 뱃지
-- (기존 BADGE / KEYWORD_BADGE 대체. 뱃지 정의는 ENUM + 애플리케이션 코드로 처리)
-- 한 스냅샷 row 가 여러 뱃지를 동시에 가질 수 있음 (1:N)
--   TREND_NEW         트렌드 랭킹 TOP100 신규 진입
--   POTENTIAL_NEW     포텐셜 랭킹 TOP100 신규 진입
--   TREND_REENTER     트렌드 랭킹 TOP100 재진입
--   POTENTIAL_REENTER 포텐셜 랭킹 TOP100 재진입
--   RISING            급상승 (한 iteration 변화량 10 이상, 임시)
--   STEADY            트렌드 랭킹 TOP100에 일주일(임시) 이상 유지
-- ------------------------------------------------------------
CREATE TABLE RANKING_SNAPSHOT_BADGE (
    id                   BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 고유 ID',
    ranking_snapshot_id  BIGINT NOT NULL COMMENT '랭킹 스냅샷 ID',
    badge_type           ENUM('TREND_NEW','POTENTIAL_NEW','TREND_REENTER','POTENTIAL_REENTER','RISING','STEADY') NOT NULL COMMENT '뱃지 종류',
    awarded_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '뱃지 부여 일시 (STEADY는 유지 시작 시점 등 팀 확정 필요)',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '레코드 생성 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_snapshot_badge (ranking_snapshot_id, badge_type),
    CONSTRAINT fk_rsb_snapshot FOREIGN KEY (ranking_snapshot_id) REFERENCES RANKING_SNAPSHOT (id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='랭킹 스냅샷-뱃지 매핑';

-- ------------------------------------------------------------
-- KEYWORD_RELATION : 키워드 연관어
-- 화면: 5 상세 검색창(연관어 검색), 6 키워드 연관어 그래프/Top1
-- ------------------------------------------------------------
CREATE TABLE KEYWORD_RELATION (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '연관어 관계 고유 ID',
    keyword_id          BIGINT        NOT NULL COMMENT '기준 키워드 ID',
    related_keyword_id  BIGINT        NOT NULL COMMENT '연관 키워드 ID',
    relevance_score     DECIMAL(6,4)  NOT NULL COMMENT '연관도 (0~1)',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '연관도 갱신 일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation_pair (keyword_id, related_keyword_id),
    KEY idx_relation_keyword_score (keyword_id, relevance_score),
    KEY idx_relation_related (related_keyword_id),
    CONSTRAINT fk_kr_keyword FOREIGN KEY (keyword_id)         REFERENCES KEYWORD (id) ON DELETE CASCADE,
    CONSTRAINT fk_kr_related FOREIGN KEY (related_keyword_id) REFERENCES KEYWORD (id) ON DELETE CASCADE,
    CONSTRAINT ck_kr_score CHECK (relevance_score BETWEEN 0 AND 1),
    CONSTRAINT ck_kr_self  CHECK (keyword_id <> related_keyword_id)
) ENGINE=InnoDB COMMENT='키워드 간 연관도 관계';
