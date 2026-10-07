# 이슈 트래커 설정

이 레포의 작업 단위는 **GitHub Issues** 다. 로컬 `.scratch/` 이슈는 쓰지 않는다.
(로컬로 잡히면 이슈가 팀원과 공유되지 않는다.)

## 규칙

- 이슈는 `to-spec` 스킬로 만들고, 형식은 `.github/ISSUE_TEMPLATE/` 를 따른다.
- 이슈를 만들면 곧바로 `/start-work {이슈번호}` 로 이슈 연결 브랜치를 만든다.
- 브랜치명은 `gh issue develop` 이 만드는 `{이슈번호}-{제목}` 형태를 그대로 쓴다. `/ship` 이 맨 앞 숫자로 이슈 번호를 읽는다.
- PR의 base는 `develop`이고, 본문에 `Closes #{이슈번호}` 를 넣는다.

## 주의

GitHub는 PR이 **기본 브랜치**에 머지될 때만 `Closes #N` 으로 이슈를 자동으로 닫는다.
`develop` 머지 시 이슈를 자동으로 닫으려면 레포의 기본 브랜치를 `develop`으로 설정해야 한다.
(Settings → General → Default branch)
