FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY apps/api/gradlew apps/api/gradlew.bat apps/api/settings.gradle.kts apps/api/build.gradle.kts ./apps/api/
COPY apps/api/gradle ./apps/api/gradle
COPY apps/api/src ./apps/api/src
WORKDIR /workspace/apps/api
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app
RUN addgroup --system spendsense && adduser --system --ingroup spendsense spendsense
COPY --from=builder /workspace/apps/api/build/libs/*.jar app.jar
USER spendsense
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=production
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
