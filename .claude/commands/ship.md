---
description: 전체 빌드를 검증한 뒤 이슈에 연결된 PR을 생성하고, 노션 명세의 Status를 "개발 완료"로 바꾸고, Auto-fix를 켠다
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git log:*), Bash(git push:*), Bash(git diff:*), Bash(gh issue view:*), Bash(gh pr create:*), Bash(gh pr view:*), Bash(./gradlew:*)
---

현재 브랜치의 작업을 PR로 올린다. 아래 순서를 그대로 따르고, 중간에 실패하면 거기서 멈춘다.

1. `git branch --show-current`로 브랜치를 확인한다.
   - `main`, `develop`이면 PR을 만들지 않고 멈춘다.
   - 브랜치명 맨 앞의 숫자를 이슈 번호 N으로 읽는다. (예: `12-ranking-api` → 12) 숫자로 시작하지 않으면 사용자에게 이슈 번호를 묻는다.
2. `git status --short`로 커밋되지 않은 변경을 확인한다. 있으면 알리고 멈춘다. (커밋은 seam별로 사람이 확인하고 한다.)
3. `./gradlew build`를 실행한다. 컴파일과 테스트를 모두 포함하며, 우리 레포의 "전체 typecheck" 단계다.
   - 실패하면 원인을 요약해 알리고 PR을 만들지 않는다.
4. `git push -u origin HEAD`로 브랜치를 올린다.
5. `gh issue view N --json title,body`로 이슈 제목과 본문을 확인하고, `.github/pull_request_template.md` 형식에 맞춰 PR을 만든다.

   ```bash
   gh pr create --base develop --assignee @me --title "<이슈 제목 그대로>" --body "<본문>"
   ```

   - PR 제목은 이슈 제목을 그대로 쓴다. (`[feat] 메인 추천 검색어 조회 API` 같은 형식을 유지한다.)
   - 본문에는 `Closes #N` 한 줄이 반드시 들어가야 한다.
   - 변경 요약은 `git log develop..HEAD` 와 `git diff develop...HEAD --stat` 기준으로 쓴다.
   - 테스트 방법과 리뷰어가 봐야 할 지점을 적는다.
6. 노션 명세 상태를 바꾼다. 이 단계가 실패해도 PR은 되돌리지 않는다.
   - 5번에서 읽은 이슈 본문에서 노션 링크(`notion.so`, `notion.com`)를 찾는다. 여러 개면 "API" 항목 근처의 링크를 우선한다.
   - 링크가 있으면 연결된 노션 도구로 그 페이지를 열어 `Status` 속성의 현재 값을 확인한다.
     - 현재 값이 `개발 중`이면 `개발 완료`로 바꾼다.
     - 그 밖의 값(`개발 완료`, `연동 완료` 등)이면 바꾸지 않고 현재 값을 알린다.
   - 링크가 없거나, 노션 도구가 없거나, 실패하면 이 단계를 건너뛰고 이유를 한 줄로 알린다.
7. Auto-fix를 켠다. CI 실패·머지 충돌·리뷰 코멘트가 생기면 앱이 이 세션을 깨워 고치게 하는 Claude 데스크톱 앱 기능이다. 이 단계가 실패해도 PR과 노션 Status 변경은 되돌리지 않는다.
   - 앱의 PR 도구(`ccd_pr`의 `get_status`, `bind_pr`, `set_monitor`)가 있으면, `get_status`로 5번에서 만든 PR이 이 세션에 연결됐는지 확인한다. 연결되지 않았으면 `bind_pr`로 PR 주소를 연결한다.
   - `set_monitor`로 그 PR의 Auto-fix를 켠다 (`auto_fix: true`, `address_comments: true`).
   - 도구가 없는 환경(터미널 CLI 등)이거나 실패하면 이 단계를 건너뛰고 이유를 한 줄로 알린다.
8. PR 주소를 알리고, 노션 Status 변경 결과, Auto-fix 결과(켰음 / 건너뜀과 이유), 다음 단계(CodeRabbit·CI 결과 확인 → 사람 리뷰)를 안내한다.
