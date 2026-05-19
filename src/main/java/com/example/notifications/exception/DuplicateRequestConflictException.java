package com.example.notifications.exception;

public class DuplicateRequestConflictException extends RuntimeException {

    public DuplicateRequestConflictException(String idempotencyKey) {
        super("Idempotency key was already used for a different request: " + idempotencyKey);
    }
}
