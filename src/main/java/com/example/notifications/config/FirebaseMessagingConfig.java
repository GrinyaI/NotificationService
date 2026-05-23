package com.example.notifications.config;

import com.example.notifications.service.delivery.FcmClient;
import com.example.notifications.service.delivery.FirebaseFcmClient;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class FirebaseMessagingConfig {

    private static final String FIREBASE_APP_NAME = "notification-service-fcm";

    private final PushProperties pushProperties;

    @Bean
    @ConditionalOnProperty(prefix = "notification.push", name = "provider", havingValue = "FCM")
    public FirebaseApp notificationFirebaseApp() throws IOException {
        return FirebaseApp.getApps().stream()
                .filter(app -> FIREBASE_APP_NAME.equals(app.getName()))
                .findFirst()
                .orElseGet(this::initializeFirebaseApp);
    }

    @Bean
    @ConditionalOnProperty(prefix = "notification.push", name = "provider", havingValue = "FCM")
    public FcmClient fcmClient(FirebaseApp notificationFirebaseApp) {
        return new FirebaseFcmClient(FirebaseMessaging.getInstance(notificationFirebaseApp));
    }

    private FirebaseApp initializeFirebaseApp() {
        PushProperties.Fcm fcm = pushProperties.getFcm();
        try (InputStream credentialsStream = credentialsStream(fcm)) {
            FirebaseOptions.Builder options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream));
            if (StringUtils.hasText(fcm.getProjectId())) {
                options.setProjectId(fcm.getProjectId());
            }
            return FirebaseApp.initializeApp(options.build(), FIREBASE_APP_NAME);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Firebase Cloud Messaging", e);
        }
    }

    private InputStream credentialsStream(PushProperties.Fcm fcm) throws IOException {
        if (StringUtils.hasText(fcm.getCredentialsPath())) {
            return Files.newInputStream(Path.of(fcm.getCredentialsPath()));
        }
        if (StringUtils.hasText(fcm.getCredentialsBase64())) {
            byte[] decoded = Base64.getDecoder().decode(fcm.getCredentialsBase64());
            return new ByteArrayInputStream(decoded);
        }
        throw new IllegalArgumentException("Firebase credentials are required when PUSH_PROVIDER=FCM");
    }
}
