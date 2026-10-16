# ADR-0005: 동적 조회에 QueryDSL(OpenFeign 포크)을 쓴다

- 상태: 확정
- 날짜: 2026-10-16
- 담당: 김송은

## 배경

키워드 상세 검색(`GET /api/v1/keywords/search`)은 검색어·카테고리·뱃지·트렌드 지수 범위·Potential 점수 범위·급상승 여부를 조합해 거르고, 세 랭킹 유형(`TREND`·`POTENTIAL`·`RISING`)의 최신 스냅샷을 함께 조인해 정렬·페이징한다. 조건이 있을 때만 붙는 동적 쿼리이고, 앞으로 다른 랭킹 화면도 비슷한 조회를 한다.

## 결정

1. **동적 조회는 QueryDSL로 작성한다.** 기존 JPQL `@Query`(최신 스냅샷 조회)도 QueryDSL로 옮겨, 조회 코드의 작성 방식을 하나로 맞춘다.
2. **배포판은 OpenFeign 포크(`io.github.openfeign.querydsl`의 `querydsl-jpa`, `querydsl-apt`, jakarta)를 쓴다.** 원조 `com.querydsl` 5.x는 유지보수가 거의 멈췄고, `orderBy` SQL 인젝션 취약점(CVE-2024-49203)이 남아 있다.
   버전은 6.x(현재 6.12)다. 7.x는 Hibernate 7 / JPA 3.2 기준이라 Spring Boot 3.5(Hibernate 6.6)와 맞지 않는다. Boot 4로 올릴 때 7.x로 함께 올린다.
3. **Q 클래스는 빌드 산출물(`build/generated`)이고 커밋하지 않는다.**

## 검토한 대안

- JPA Criteria / Specification: 의존성은 늘지 않지만 코드가 장황하고 읽기 어렵다. 조인과 서브쿼리가 많은 검색 쿼리에서 특히 그렇다.
- 문자열 JPQL을 조건별로 이어 붙이기: 타입 검사가 없고, 실수로 인젝션 여지가 생긴다.
- 원조 `com.querydsl` 5.x: 자료는 많지만 위 취약점과 유지보수 상태 때문에 제외했다.

## 결과

- 빌드 설정에 QueryDSL 의존성과 annotation processor가 추가된다.
- 정렬 조건을 사용자 입력에서 만들 때는 허용된 값(enum)으로만 매핑한다. 문자열을 그대로 `orderBy`에 넣지 않는다.
- 포크의 유지보수가 멈추면 이 ADR을 다시 검토한다.
