# ADR-0002: 백엔드 스택

- 상태: 확정 (세부 버전은 초기 설정값)
- 날짜: 2026-10-07
- 담당: 김송은

## 결정

- 메인 API 서버는 **Spring Boot + Spring Data JPA (Java)** 로 만든다. AI 추론은 별도 FastAPI 서버가 맡는다.
- DB는 **MySQL**이다. 스키마의 기준은 `docs/schema.sql`이다.
- 빌드는 **Gradle**.

## 초기 설정값 (필요하면 바꾼다)

| 항목 | 값 | 비고 |
| --- | --- | --- |
| Java | 21 | 로컬 JDK가 다르면 `build.gradle`의 toolchain 수정 |
| Spring Boot | 3.5.6 | 새 버전으로 올릴 때는 별도 이슈로 진행 |
| 패키지 | `com.djk` | 도메인별 `domain.<도메인>` 안에 `controller`·`service`·`repository`·`entity`·`dto` 계층, 공통은 `global.*` (2026-10-16 PR #2 리뷰로 확정) |

## 근거

- 팀이 이미 Spring Boot(Java) 메인 서버 + FastAPI AI 서버 구성으로 설계했다.
- 이 스키마는 FK 관계와 ENUM이 많아서 JPA 엔티티로 옮기기 쉽다.

## 추가 결정 (2026-10-14, 이슈 #1)

### 통합 테스트는 Testcontainers MySQL 로 한다

- `@SpringBootTest` 통합 테스트는 H2 대신 Testcontainers 로 `mysql:8.0` 을 띄우고 `docs/schema.sql` 을 그대로 적용한다. 설정은 `docker-compose.yml` 과 같다.
- 이유: 스키마가 MySQL ENUM·CHECK 를 쓰고 `ddl-auto=validate` 이므로, 엔티티와 스키마가 맞는지는 실제 MySQL 에서만 검증할 수 있다. H2 는 이 둘을 다르게 해석한다.
- 테스트를 돌리려면 로컬과 CI 모두 Docker 가 필요하다. GitHub Actions `ubuntu-latest` 에는 기본으로 있다.

### Testcontainers 버전을 1.21.4 로 고정한다

- Spring Boot 3.5.6 이 관리하는 Testcontainers 는 1.21.3 이다. 이 버전은 Docker Engine 29 (API 1.56, 최소 API 1.40) 에서 컨테이너를 띄우지 못한다. 실제로 겪은 에러는 아래와 같다.
  ```
  IllegalStateException: Could not find a valid Docker environment
    UnixSocketClientProviderStrategy: ... NoSuchFileException (/var/run/docker.sock)
    DockerDesktopClientProviderStrategy: ... BadRequestException (Status 400: {"ID":"", ... })
  ```
  1.21.3 의 docker-java 가 Docker 29 의 최소 API 버전보다 낮은 버전으로 요청해서 400 을 받는다.
- `build.gradle` 의 `ext['testcontainers.version'] = '1.21.4'` 로 버전만 올린다. `~/.docker-java.properties` 같은 개인 설정으로 우회하지 않는다. CI 러너의 Docker 가 올라가도 같은 문제가 생기기 때문이다.
- **제거 시점**: Spring Boot 를 올렸을 때 Boot 가 관리하는 Testcontainers 버전이 1.21.4 이상이면 이 `ext` 줄을 지운다.

### JPA 물리 네이밍 전략은 매핑한 이름을 그대로 쓴다

- `spring.jpa.hibernate.naming.physical-strategy = PhysicalNamingStrategyStandardImpl`
- 이유: Spring Boot 기본값(`CamelCaseToUnderscoresNamingStrategy`)은 `@Table(name = "KEYWORD")` 를 `keyword` 로 소문자화한다. Linux 의 MySQL 은 테이블명 대소문자를 구분하므로(`lower_case_table_names=0`) 대문자 테이블을 찾지 못한다. 실제로 겪은 에러: `Schema-validation: missing table [keyword]`. 운영 DB 도 Linux 라면 같은 문제가 생긴다.
- **규칙**: 엔티티는 테이블명을 `@Table(name = "...")`, 컬럼명을 `@Column(name = "...")` / `@JoinColumn(name = "...")` 으로 모두 명시한다. camelCase → snake_case 자동 변환은 없다. 빠뜨리면 `ddl-auto=validate` 가 기동할 때 "missing column" 으로 잡는다.

### DATETIME 은 KST 벽시계 값으로 보고, 시간대 변환을 하지 않는다

- DB 의 `DATETIME` 은 KST 기준 시각이고, 엔티티에서는 `LocalDateTime` 으로 변환 없이 매핑한다. `hibernate.jdbc.time_zone` 은 설정하지 않는다.
- 이유: `hibernate.jdbc.time_zone: Asia/Seoul` 이 있으면 Hibernate 가 `LocalDateTime` 을 JVM 기본 시간대의 시각으로 보고 변환한다. 그래서 JVM 이 UTC 인 환경(GitHub Actions, 보통의 Docker 이미지)에서는 값이 9시간 어긋난다. 실제로 겪은 문제: DB 에 `2026-07-24 09:00:00` 으로 넣은 `snapshot_at` 이 API 에서 `2026-07-24T00:00:00` 으로 나왔다. 로컬(KST)에서는 통과하고 CI(UTC)에서만 실패했다.
- 테스트 JVM 은 `user.timezone=UTC` 로 고정해서, 시간대에 기대는 코드를 로컬에서도 잡는다.
