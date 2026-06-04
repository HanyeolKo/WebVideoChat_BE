# ── build stage ──────────────────────────────────────────────
# 프로젝트의 gradle 래퍼를 사용해 버전 일관성을 유지한다.
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean build --no-daemon

# ── run stage ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# plain jar는 build.gradle에서 비활성화되어 있어 실행 가능한 boot jar 하나만 남는다.
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
