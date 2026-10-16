# CLAUDE.md — DJK 트렌드 분석기 백엔드

## 프로젝트

건국대학교 컴퓨터공학부 졸업 프로젝트 "DJK"의 **메인 API 서버**다. 여러 소스(YouTube, 네이버 블로그/카페 등)의 신호로 트렌드 키워드를 탐지·예측하는 서비스이며, AI 추론은 별도의 FastAPI 서버가 맡는다(이 레포의 범위가 아니다).

- 스택: Java 21 · Spring Boot 3.5 · Gradle · Spring Data JPA · MySQL 8.0.16+
- 도메인 용어는 `CONTEXT.md`, 설계 결정은 `docs/adr/`, DB 스키마의 기준은 `docs/schema.sql` 이다.
- 코드를 쓰기 전에 위 문서와 기존 코드를 먼저 읽는다.

## 자주 쓰는 명령

```bash
./gradlew build        # 컴파일 + 테스트. CI와 /ship 이 사용하는 "typecheck" 단계
./gradlew test
./gradlew bootRun      # DB_PASSWORD 환경변수 필요
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # Swagger UI: http://localhost:8080/swagger-ui/index.html
docker compose up -d   # 로컬 MySQL (docs/schema.sql 자동 적용)
```

## 프로젝트 규칙

### API

- base path는 `/api/v1`.
- 모든 응답은 `ApiResponse`(`isSuccess` / `message` / `details`)로 감싼다.
- 트렌드 지수는 DB에 0~1(`DECIMAL(6,4)`)로 저장하고, API 응답에서는 0~100으로 환산해 내려준다. 환산은 응답 DTO를 만드는 곳 한 군데에서만 한다.
- API 명세는 노션 ["API 명세서"](https://app.notion.com/p/kuclubunion/DJK-API-931991323bb783b290bf81b6f03ab321?source=copy_link) 페이지가 기준이다. 구현 전에 해당 엔드포인트 페이지를 읽는다.
- 모든 API에 Swagger 문서를 붙인다. 컨트롤러에 `@Tag`, 엔드포인트에 `@Operation`(summary·description), 파라미터에 `@Parameter`, 응답 코드별 예시를 단다. Swagger는 실제 구현을 보여주는 문서이고, 명세의 기준은 노션이다. 둘이 다르면 노션에 맞춘다.
- Swagger UI와 `/v3/api-docs`는 `local`·`dev` 프로파일에서만 켜진다. 운영에서는 꺼져 있다.

### DB

- 스키마 변경은 `docs/schema.sql` 수정 + ADR 갱신으로 한다. `ddl-auto`는 `validate`를 유지하고, Hibernate가 테이블을 만들게 하지 않는다.
- 랭킹 조회와 "직전 스냅샷과 비교"는 반드시 `snapshot_at` 기준이다. `created_at`은 INSERT 시각이라 비교·정렬 기준으로 쓰지 않는다.
- 테이블명은 대문자이고 `TB_` 접두사는 쓰지 않는다.
- 뱃지는 별도 마스터 테이블 없이 `RANKING_SNAPSHOT_BADGE.badge_type`(ENUM)이다. 부여 조건 판정은 애플리케이션 코드에서 한다.
- 엔티티의 ENUM 필드는 `@Enumerated(EnumType.STRING)`을 쓴다.

### 코드

- 패키지는 기능(도메인)별로 나누고, 도메인 안은 계층별로 나눈다. 공통 코드는 `com.djk.global` 에 둔다.
  ```text
  com.djk.domain.<도메인>/
  ├── controller/
  ├── service/
  ├── repository/
  ├── entity/      # JPA 엔티티와 엔티티가 쓰는 ENUM
  └── dto/         # 요청·응답 DTO, 계층 사이에 넘기는 조회 결과
  ```
  필요한 계층만 만든다. 빈 패키지는 두지 않는다. 테스트는 테스트 대상과 같은 패키지에 둔다.
- 비밀번호, 토큰 등 비밀 값은 코드와 설정 파일에 넣지 않고 환경변수로 받는다.

## 작업 방식 (개발 파이프라인)

정보운영팀의 개발 파이프라인을 따른다. 목표는 AI를 "잘" 쓰고, 누가 작업해도 결과물이 한 사람이 쓴 것처럼 보이게 하는 것이다.

### 기본 원칙

- "이거 구현해줘"처럼 바로 구현하지 않는다. 먼저 레포 문서와 기존 코드를 본다.
- 한 번에 한 작업만 한다.
- 요구가 애매하면 구현하지 말고 먼저 사용자와 정렬한다.
- 결정은 말로만 두지 않고 ADR에 남긴다. 에이전트가 초안을 쓰고 사람이 확인한다.
- 새 용어가 생기거나 중요한 설계 결정을 내렸을 때만 `CONTEXT.md` / ADR을 갱신한다. 문서 작업 자체가 목표가 아니다.
- 노션 API 명세의 Status는 커맨드가 바꾼다.
  - `/start-work`: "개발 전" 또는 "수정 필요" → "개발 중"
  - `/ship`(PR 생성 시): "개발 중" → "개발 완료"
  - 리뷰에서 큰 수정이 생기거나 연동 중 문제가 나오면 사람이 "수정 필요"로 되돌린다.

### 흐름

```text
아이디어 / 요청
  → grill-with-docs   요구사항 · 용어 정렬          (스킬)
  → to-spec           GitHub Issue 생성             (스킬)
  → /start-work {이슈번호}   이슈 연결 브랜치 생성·체크아웃   (커맨드)
  → /implement        seam 별로 tdd 루프 반복, 끝나면 code-review 로 인계   (스킬)
  → /ship             전체 빌드 검증 → PR 생성 (Closes #이슈번호)  (커맨드)
  → CodeRabbit 리뷰 + CI → 사람 리뷰 → approve → merge(develop)
```

- 이슈가 이미 명확하면 정렬 단계를 짧게 하고 바로 구현해도 된다.
- 이슈 하나가 seam 하나 정도로 작으면 `/implement` 대신 `tdd`를 직접 불러도 되지만, 그때는 `code-review` 인계를 직접 챙긴다.
- **이슈를 발행한 직후에는 곧바로 `/start-work {이슈번호}`를 실행한다.**
- 커밋은 seam(의미 있는 작은 단위)별로 한다.

### 스킬과 커맨드

- **커맨드**(`.claude/commands/*.md`): 사람이 `/이름`으로 직접 호출하는 고정 절차. 매번 같은 `git`/`gh` 명령을 실행한다.
- **스킬**(`.claude/skills/<이름>/SKILL.md`): 작업 맥락과 description을 보고 Claude가 스스로 꺼내 쓰는 지침. 판단이 필요한 작업(정렬, 스펙, 구현 절차)에 쓴다.

| 이름 | 종류 | 언제 |
| --- | --- | --- |
| `grill-with-docs` | 스킬 | 새 기능 시작, 요구사항이 애매하거나 설계 선택이 필요할 때 |
| `to-spec` | 스킬 | 정렬한 내용을 GitHub Issue로 고정할 때 |
| `/start-work` | 커맨드 | 이슈 발행 직후, 작업용 브랜치가 필요할 때 |
| `/implement` | 스킬 | 실제 구현. seam 판단 → tdd 루프 → code-review 인계까지 안에서 처리 |
| `/ship` | 커맨드 | 구현이 끝나 PR을 올릴 때 |
