# ADR-0006: EC2에서 Docker Compose로 돌리고, develop CI가 통과하면 SSH로 자동 배포한다

- 상태: 제안
- 날짜: 2026-11-23
- 담당: 김송은

## 배경

백엔드를 AWS에 올렸다. 앱은 EC2 한 대, DB는 RDS(MySQL)다. 프론트 연동이 시작돼서 develop에 머지된 내용이 사람 손을 거치지 않고 서버에 반영돼야 한다. 졸업 프로젝트 규모라 서버는 한 대이고, 배포 인프라에 비용과 운영 부담을 크게 들이지 않는다.

## 결정

1. **앱은 Docker 이미지로 만들어 EC2에서 `docker-compose.prod.yml`로 실행한다.**
   - `Dockerfile`은 멀티 스테이지다. JDK 이미지에서 `bootJar`를 만들고 JRE 이미지(Java 21)에서 실행한다. 서버에 JDK를 깔지 않는다.
   - 이미지 빌드에서는 테스트를 건너뛴다(`-x test`). 테스트는 Testcontainers가 필요해 CI가 맡는다.
   - 컨테이너 포트 8080을 호스트 80으로 연다.
2. **DB는 RDS를 쓰고, 접속 정보는 EC2 레포 루트의 `.env`로만 받는다.** `.env`는 gitignore 대상이고 이미지에도 들어가지 않는다(`.dockerignore`). 값이 빠지면 compose가 시작하지 않는다.
3. **배포는 GitHub Actions `Deploy` 워크플로가 한다.** develop에 push된 CI가 성공하면(`workflow_run`) SSH로 EC2에 접속해 `git reset --hard origin/develop` → `docker compose up -d --build`를 실행한다. PR의 CI로는 배포하지 않는다. Actions 탭에서 수동 실행도 된다.
   - 배포 후 `GET /api/v1/keywords/recommended`가 2분 안에 응답하지 않으면 실패로 보고 컨테이너 로그를 남긴다.
   - 접속 정보는 GitHub Secrets(`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`)에 둔다.
4. **운영은 기본 프로파일로 띄워 Swagger를 끈다.** 연동 중 Swagger가 필요할 때만 `.env`에 `SPRING_PROFILES_ACTIVE=dev`를 넣는다.
5. **RDS 파라미터 그룹의 `time_zone`은 `Asia/Seoul`이다.** ADR-0002의 "DATETIME은 KST 벽시계 값" 기준을 운영 DB에서도 지키기 위해서다.

## 검토한 대안

- 이미지 레지스트리(ECR, GHCR)에 올리고 서버는 pull만 하기: 서버에서 빌드하지 않아 배포가 빠르고 롤백이 쉽지만, 레지스트리 설정과 인증이 늘어난다. 서버 한 대 규모에서는 이득이 작다.
- AWS CodeDeploy / Elastic Beanstalk / ECS: 관리형이라 기능은 많지만 설정과 비용이 프로젝트 규모에 비해 크다.
- 서버에서 직접 `java -jar` 실행: 서버에 JDK와 프로세스 관리(systemd 등)를 따로 갖춰야 하고, 로컬과 실행 환경이 달라진다.

## 결과

- develop 머지가 곧 운영 배포다. develop의 CI가 깨지면 배포도 멈춘다.
- 빌드를 EC2에서 하므로 배포 중 서버 CPU·메모리를 쓴다. 인스턴스가 작으면 배포가 느려지거나 메모리가 부족할 수 있다. 그때는 레지스트리 방식으로 바꾼다.
- 탄력적 IP가 없어 인스턴스를 중지·시작하면 IP가 바뀐다. 그때마다 `EC2_HOST` Secret을 고친다.
- 무중단 배포가 아니다. 컨테이너를 교체하는 동안 잠깐 응답이 끊긴다.
- 워크플로는 항상 develop 최신 커밋을 배포한다. 롤백은 develop에 revert 커밋을 머지하는 방식이다.
