---
description: GitHub 이슈에 연결된 브랜치를 만들고 체크아웃한 뒤, 노션 명세의 Status를 "개발 중"으로 바꾼다
argument-hint: <이슈번호>
allowed-tools: Bash(gh issue view:*), Bash(gh issue develop:*), Bash(git status:*), Bash(git fetch:*), Bash(git branch:*), Bash(git checkout:*)
---

이슈 #$ARGUMENTS 작업을 시작한다. 아래 순서를 그대로 따른다.

1. `$ARGUMENTS`가 숫자 하나인지 확인한다. 아니면 `/start-work 12` 형태로 사용하라고 알리고 멈춘다.
2. `git status --short`로 커밋되지 않은 변경(새 파일 포함)이 있는지 본다. 있으면 목록을 보여주고, 커밋하거나 stash한 뒤 다시 실행하라고 안내하고 **멈춘다.** 사용자가 확인하기 전에는 브랜치를 바꾸지 않는다.
3. `gh issue view $ARGUMENTS --json title,state,body`로 제목, 상태, 본문을 확인한다. 이미 closed 상태면 알리고 계속할지 묻는다.
4. `gh issue develop $ARGUMENTS --list`로 이 이슈에 연결된 브랜치가 이미 있는지 본다.
   - 있으면 새로 만들지 말고 `git fetch origin` 후 그 브랜치를 체크아웃한다. 5번은 건너뛴다.
5. 브랜치 이름을 **직접 정해서** 만든다. 이름을 `gh`에 맡기지 않는다. (한글 제목이면 번호만 남거나 이름이 이상해질 수 있다.)
   - 이름 형식: `{이슈번호}-{영문-슬러그}` 예) `1-recommended-keywords-api`
   - 슬러그: 이슈 제목을 영어로 요약한 2~5단어, 소문자·숫자·하이픈만 사용한다. 한글, 공백, 특수문자는 쓰지 않는다. 제목 앞의 `[feat]` 같은 `[타입]` 접두사는 슬러그에 넣지 않는다.
   - `git fetch origin` 후 아래를 실행한다.

     ```bash
     gh issue develop $ARGUMENTS --base develop --name "{이슈번호}-{영문-슬러그}" --checkout
     ```

   - 실행 후 `git branch --show-current`로 현재 브랜치가 이슈 번호로 시작하는지 확인한다. 아니면 고치려 하지 말고 그대로 사용자에게 알린다. (`/ship`이 맨 앞 숫자로 이슈 번호를 읽는다.)
6. 노션 명세 상태를 바꾼다. 이 단계가 실패해도 브랜치 작업은 되돌리지 않는다.
   - 3번에서 읽은 이슈 본문에서 노션 링크(`notion.so`, `notion.com`)를 찾는다. 여러 개면 "API" 항목 근처의 링크를 우선한다.
   - 링크가 있으면 연결된 노션 도구로 그 페이지를 열어 `Status` 속성의 현재 값을 확인한다.
     - 현재 값이 `개발 전` 또는 `수정 필요`이면 `개발 중`으로 바꾼다.
     - 그 밖의 값(`개발 중`, `개발 완료`, `연동 완료`, `스웨거 반영 전`)이면 바꾸지 않고 현재 값을 알린다.
   - 링크가 없거나, 노션 도구가 없거나, 실패하면 이 단계를 건너뛰고 이유를 한 줄로 알린다.
7. 결과를 짧게 알린다.
   - 체크아웃된 브랜치명
   - 이슈 제목
   - 노션 Status 변경 결과 (바꿨음 / 이미 해당 값 / 건너뜀과 이유)
   - 다음 단계: `/implement`
