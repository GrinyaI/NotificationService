package com.example.notifications.service.delivery;

public class PushDeliveryException extends RuntimeException {

    private final boolean invalidToken;

    public PushDeliveryException(String message) {
        this(message, null, false);
    }

    public PushDeliveryException(String message, Throwable cause, boolean invalidToken) {
        super(message, cause);
        this.invalidToken = invalidToken;
    }

    public boolean isInvalidToken() {
        return invalidToken;
    }
}
