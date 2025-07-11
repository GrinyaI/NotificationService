package com.example.notifications;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableCaching
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "Notification Service API",
                version = "1.0",
                description = "API для создания, просмотра и управления уведомлениями по каналам EMAIL, SMS и PUSH"
        ),
        servers = {
                @Server(url = "/", description = "Локальный сервер")
        }
)
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
