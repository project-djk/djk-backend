# DJK 트렌드 분석기 — Backend

건국대학교 컴퓨터공학부 졸업 프로젝트 "DJK"의 메인 API 서버 (Spring Boot + JPA + MySQL).

## 시작하기

### 1. DB

```bash
cp .env.example .env      # DB_PASSWORD 값을 바꾼다
docker compose up -d      # MySQL 8.0 + docs/schema.sql 자동 적용
```

### 2. 실행

```bash
export DB_PASSWORD=...    # .env 와 같은 값 (IntelliJ 라면 실행 설정의 환경변수)
./gradlew bootRun
./gradlew build           # 컴파일 + 테스트
```

## 구조

```text
CLAUDE.md                  Claude Code 작업 규칙 (개발 파이프라인 포함)
CONTEXT.md                 도메인 용어집
docs/schema.sql            DB 스키마 기준
docs/adr/                  설계 결정 기록
docs/agents/               이슈 트래커 설정
.claude/commands/          /start-work, /ship
.github/                   이슈·PR 템플릿, CI
src/main/java/com/djk/     애플리케이션 코드
```

## 개발 파이프라인

```text
grill-with-docs → to-spec(GitHub Issue) → /start-work → /implement → /ship → 리뷰 → merge(develop)
```

자세한 규칙은 `CLAUDE.md`를 본다.

## 레포 세팅 체크리스트 (레포당 1회)

- [ ] GitHub 레포 생성, 기본 브랜치를 `develop`으로 설정 (`Closes #N` 자동 close에 필요)
- [ ] Gradle Wrapper 생성 후 커밋
- [ ] Claude Code에서 `/setup-matt-pocock-skills` 실행 (이슈 트래커는 GitHub로)
  - 이슈 트래커 설정이 `docs/agents/issue-tracker.md`와 같은지 확인 (로컬 `.scratch/`로 잡히면 수정)
- [ ] `.claude/skills/to-spec/SKILL.md`의 내장 템플릿을 `.github/ISSUE_TEMPLATE/feature.md` 형식으로 교체
- [ ] CodeRabbit GitHub App을 이 레포에 설치
- [ ] 첫 `/start-work`, `/ship` 으로 동작 확인 (`--assignee @me`, `Closes #이슈번호` 포함 여부)
