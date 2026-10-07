---
description: 전체 빌드를 검증한 뒤 이슈에 연결된 PR을 생성한다
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
5. `gh issue view N`으로 이슈 제목을 확인하고, `.github/pull_request_template.md` 형식에 맞춰 PR을 만든다.

   ```bash
   gh pr create --base develop --assignee @me --title "<이슈 제목 기반 제목>" --body "<본문>"
   ```

   - 본문에는 `Closes #N` 한 줄이 반드시 들어가야 한다.
   - 변경 요약은 `git log develop..HEAD` 와 `git diff develop...HEAD --stat` 기준으로 쓴다.
   - 테스트 방법과 리뷰어가 봐야 할 지점을 적는다.
6. PR 주소를 알리고, 다음 단계(CodeRabbit·CI 결과 확인 → 사람 리뷰)를 안내한다.
