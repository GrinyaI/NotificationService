package com.example.notifications.service.delivery;

import java.util.Map;

public interface FcmClient {

    String send(String token, String title, String body, Map<String, String> data);
}
