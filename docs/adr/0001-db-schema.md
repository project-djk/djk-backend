# ADR-0001: DB 스키마 확정

- 상태: 확정
- 날짜: 2026-10-07
- 담당: 김송은
- 기준 파일: `docs/schema.sql`

## 배경

초안 스키마에 FK 타입 불일치(VARCHAR vs BIGINT), 시계열 테이블의 단독 UNIQUE, 뱃지 구조 불일치가 있어 백엔드 개발 전에 정리했다.

## 결정

1. **뱃지는 `RANKING_SNAPSHOT_BADGE` + ENUM 으로 간다.**
   뱃지는 키워드가 아니라 "그 시점의 랭킹"에 붙는 정보이므로 `ranking_snapshot_id`에 매핑한다. 뱃지 마스터(`BADGE`)와 `KEYWORD_BADGE`는 폐기한다. 부여 조건은 DB가 아니라 애플리케이션 코드에서 판정한다.
2. **시간 컬럼 두 개를 모두 유지한다.**
   `snapshot_at`은 논리적 집계 시점, `created_at`은 물리적 INSERT 시각이다. 조회와 비교는 `snapshot_at` 기준이다.
3. **트렌드 지수는 0~1 정규화 값(`DECIMAL(6,4)`)으로 저장한다.** 화면·API에서는 100점으로 환산한다.
4. **집계 주기는 1일 1회로 시작한다.** 성능이나 급상승 판정 기준에 문제가 있으면 바꾼다.
5. **UNIQUE는 복합 키로 둔다.**
   - `RANKING_SNAPSHOT (ranking_type, snapshot_at, keyword_id)`
   - `TREND_INDEX_HISTORY (keyword_id, measured_at)`
   - `KEYWORD_RELATION (keyword_id, related_keyword_id)`
6. **테이블명은 `TB_` 접두사 없이 통일한다.**
7. **무결성은 DB가 지킨다.** FK는 `ON DELETE CASCADE`, 점수는 0~1, 순위는 1~100 CHECK, 자기 자신과의 연관어는 CHECK로 막는다.

## 미정 (결정 시 이 ADR 또는 새 ADR에 반영)

- `RISING`: 랭킹 유형(Rising Trend 화면)과 뱃지가 어떻게 구분되는지
- `STEADY` 뱃지의 `awarded_at` 의미 (부여 시각 vs 유지 시작 시점)
- 카테고리의 개수와 종류 (초기 데이터)
- 로그인 방식: JWT 자체 발급 vs Google OAuth
- 카테고리 필터가 단일 선택인지 다중 선택인지
- 뱃지 검색을 상세 검색 API 안에서 처리할지, 별도 엔드포인트로 둘지

## 결과

- 스키마의 기준은 `docs/schema.sql`이며 `ddl-auto`는 `validate`로 둔다.
- 스키마를 바꾸면 `docs/schema.sql`과 이 ADR(또는 후속 ADR)을 함께 수정한다.
