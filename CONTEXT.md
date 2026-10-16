# CONTEXT — 도메인 용어집

코드, 이슈, 대화에서 같은 말을 같은 뜻으로 쓰기 위한 문서다. 새 용어가 생기면 여기에 추가한다.

## 핵심 개념

| 용어 | 뜻 | 코드/DB |
| --- | --- | --- |
| 키워드 | 서비스가 수집·추적하는 트렌드 후보 단어 | `KEYWORD` |
| 트렌드 지수 | 키워드의 트렌드 강도. DB는 0~1, API는 0~100 | `trend_score` |
| 집계(iteration) | 랭킹을 한 번 계산하는 배치. 현재 1일 1회 | `snapshot_at` |
| 스냅샷 | 한 번의 집계 결과로 저장된 랭킹 row 묶음. 엔티티 `RankingSnapshot` 은 스냅샷 전체가 아니라 스냅샷의 row 하나다 | `RANKING_SNAPSHOT` |
| 최신 스냅샷 | 한 랭킹 유형에서 `snapshot_at`이 가장 늦은 스냅샷. 집계는 통째로 저장되므로 항상 완결된 결과다 | `snapshot_at` |
| 추천 검색어 | 메인 화면에 노출되는 키워드 목록. `TREND` 최신 스냅샷의 1위부터 순서대로 N개(기본 10)이며, 걸러내는 규칙은 없다 | `/keywords/recommended` |
| 연관도 | 두 키워드가 얼마나 관련 있는지 (0~1) | `relevance_score` |
| Potential 점수 | `POTENTIAL` 스냅샷 row에 기록된 트렌드 지수. 스케일은 트렌드 지수와 같다 | `potential.score` |
| 순위 변화 | 같은 랭킹 유형의 직전 스냅샷 순위 − 현재 순위. 양수가 상승이다. 직전 스냅샷에 없었으면 값이 없다 | `rankChange` |
| 검색 대상 키워드 | 최신 `TREND`·`POTENTIAL`·`RISING` 스냅샷 중 하나 이상에 든 키워드. 상세 검색은 이 안에서만 찾는다 | `/keywords/search` |

## 랭킹 유형 (`ranking_type`)

| 값 | 화면 | 설명 |
| --- | --- | --- |
| `TREND` | 2 트렌드 랭킹 | 트렌드 지수 기준 TOP100. 메인 추천 검색어(TOP10)도 이 값의 최신 스냅샷에서 가져온다 |
| `RISING` | 3 Rising Trend | 변화량이 큰 키워드. 급상승 뱃지와의 구분은 팀 확정 필요 |
| `POTENTIAL` | 4 Potential Trend | 잠재 트렌드 |

## 뱃지 (`badge_type`)

랭킹 스냅샷 row에 붙는다. 한 row가 여러 뱃지를 동시에 가질 수 있다.

| 값 | 조건 |
| --- | --- |
| `TREND_NEW` | 트렌드 랭킹 TOP100에 신규 진입 |
| `POTENTIAL_NEW` | 포텐셜 랭킹 TOP100에 신규 진입 |
| `TREND_REENTER` | 트렌드 랭킹 TOP100에 재진입 |
| `POTENTIAL_REENTER` | 포텐셜 랭킹 TOP100에 재진입 |
| `RISING` | 한 iteration 변화량이 10 이상 (임시 기준) |
| `STEADY` | 트렌드 랭킹 TOP100에 일주일 이상 유지 (임시 기준) |

## 헷갈리기 쉬운 것

- **신규 진입 vs 재진입**: 직전 스냅샷에 없었더라도 과거에 순위권에 든 적이 있으면 재진입이다. 순위권 이탈 일자는 `KEYWORD.trend_last_ranked_out_at` / `potential_last_ranked_out_at` 에 기록한다.
- **`snapshot_at` vs `created_at`**: `snapshot_at`은 "어느 집계의 결과인지"(한 집계의 모든 row가 같은 값), `created_at`은 "DB에 실제로 들어간 시각"이다. 조회와 비교는 `snapshot_at` 기준이다.
- **점수 스케일**: DB 0~1, API 0~100. 변환 위치는 응답 DTO 생성부 한 곳이다.
