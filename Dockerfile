FROM eclipse-temurin:17-jre-alpine
LABEL authors="grine"

WORKDIR /app

COPY target/notification-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]