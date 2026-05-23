package com.example.notifications.service.delivery;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class FirebaseFcmClient implements FcmClient {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public String send(String token, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();
        try {
            return firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            throw new PushDeliveryException(
                    "Firebase Cloud Messaging request failed: " + e.getMessagingErrorCode(),
                    e,
                    isInvalidToken(e)
            );
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        return exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED;
    }
}
