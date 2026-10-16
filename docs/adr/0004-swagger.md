# ADR-0004: Swagger(OpenAPI) 도입과 노출 범위

- 상태: 확정
- 날짜: 2026-10-16
- 담당: 김송은
- 관련: 이슈 #4, PR #2 리뷰

## 배경

PR #2 리뷰에서 앞으로 모든 API에 Swagger 문서를 붙이기로 했다. API 명세는 이미 노션 "API 명세서"에 있으므로, 둘 중 무엇이 기준인지와 운영 환경 노출 여부를 정해야 했다.

## 결정

1. **springdoc-openapi(`springdoc-openapi-starter-webmvc-ui`)를 쓴다.** 버전은 Spring Boot 3.5와 호환되는 2.8.13이다.
2. **명세의 기준은 노션 "API 명세서"다.** Swagger는 실제 구현을 보여주는 문서다. 둘이 다르면 노션에 맞춰 구현과 Swagger를 고친다.
3. **Swagger UI와 `/v3/api-docs`는 `local`·`dev` 프로파일에서만 켠다.** 기본값은 꺼짐이다. 그래서 운영에서 프로파일 지정을 빠뜨려도 문서가 노출되지 않는다.
4. **프로파일별 Swagger 설정은 `application.yml` 안의 `on-profile: local | dev` 문서에 둔다.** `application-local.yml`은 `.gitignore` 대상(개인 설정)이므로 팀 공통 설정을 그 파일에 두면 공유되지 않는다.
5. **모든 API에 문서를 붙인다.** 컨트롤러에 `@Tag`, 엔드포인트에 `@Operation`, 파라미터에 `@Parameter`, 응답 코드별 예시를 단다. springdoc의 `@ApiResponse`는 우리 `com.djk.global.response.ApiResponse`와 이름이 같으므로 전체 이름(`io.swagger.v3.oas.annotations.responses.ApiResponse`)으로 쓴다.

## 검토한 대안

- 운영에서도 노출하고 인증으로 막기: 지금은 로그인 기능이 없어서(ADR-0001 8번) 막을 수단이 없다.
- 기본값을 켜 두고 `prod` 프로파일에서 끄기: 프로파일을 빠뜨리면 운영에 노출된다.
- Swagger를 명세의 기준으로 삼기: 노션 명세가 이미 팀과 프론트의 기준이고 Status로 진행 상황도 관리하고 있다.

## 결과

- 로컬 실행: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` 후 `http://localhost:8080/swagger-ui/index.html`
- 테스트가 "프로파일이 없으면 404, local·dev 이면 문서가 노출됨"을 검증한다.
