FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-alpine

LABEL authors="Grinevich Ilya"

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/NotificationService.jar app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

USER app

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:${SERVER_PORT:-8080}/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
