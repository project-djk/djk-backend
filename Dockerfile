# 운영(EC2) 배포용 이미지. 빌드는 컨테이너 안에서 하므로 서버에 JDK 를 깔 필요가 없다.
# 테스트는 Testcontainers(도커 안의 도커)가 필요해 여기서는 건너뛴다. 테스트는 CI 가 맡는다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 레이어를 먼저 캐시해서 코드만 바뀌었을 때 재빌드를 빠르게 한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
