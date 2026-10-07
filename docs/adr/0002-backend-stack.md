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
| 패키지 | `com.djk` | 도메인별 `domain.*`, 공통은 `global.*` |

## 근거

- 팀이 이미 Spring Boot(Java) 메인 서버 + FastAPI AI 서버 구성으로 설계했다.
- 이 스키마는 FK 관계와 ENUM이 많아서 JPA 엔티티로 옮기기 쉽다.
