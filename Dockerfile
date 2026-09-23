# ---- Stage 1: build ----
FROM amazoncorretto:21-alpine AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew && ./gradlew :app-service:bootJar --no-daemon

# ---- Stage 2: runtime ----
FROM amazoncorretto:21-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /runtime

COPY --from=build --chown=app:app /workspace/applications/app-service/build/libs/*.jar app.jar

USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/runtime/app.jar"]