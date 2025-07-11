FROM eclipse-temurin:17-jre-alpine
LABEL authors="Grinevich Ilya"

WORKDIR /app

COPY target/NotificationService.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]