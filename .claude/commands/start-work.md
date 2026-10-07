---
description: GitHub 이슈에 연결된 브랜치를 만들고 체크아웃한다
argument-hint: <이슈번호>
allowed-tools: Bash(gh issue view:*), Bash(gh issue develop:*), Bash(git status:*), Bash(git fetch:*), Bash(git branch:*), Bash(git checkout:*)
---

이슈 #$ARGUMENTS 작업을 시작한다. 아래 순서를 그대로 따른다.

1. `$ARGUMENTS`가 숫자 하나인지 확인한다. 아니면 `/start-work 12` 형태로 사용하라고 알리고 멈춘다.
2. `git status --short`로 커밋되지 않은 변경이 있는지 본다. 있으면 목록을 보여주고, 사용자가 확인하기 전에는 브랜치를 바꾸지 않는다.
3. `gh issue view $ARGUMENTS`로 제목과 상태를 확인한다. 이미 closed 상태면 알리고 계속할지 묻는다.
4. `gh issue develop $ARGUMENTS --list`로 이 이슈에 연결된 브랜치가 이미 있는지 본다.
   - 있으면 새로 만들지 말고 그 브랜치를 `git fetch origin` 후 체크아웃한다.
   - 없으면 `git fetch origin`을 하고 `gh issue develop $ARGUMENTS --base develop --checkout` 을 실행한다.
5. 결과를 짧게 알린다.
   - 체크아웃된 브랜치명 (이슈 번호로 시작해야 `/ship`이 번호를 읽을 수 있다)
   - 이슈 제목
   - 다음 단계: `/implement`
