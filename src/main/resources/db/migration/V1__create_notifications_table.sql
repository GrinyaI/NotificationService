CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE notifications
(
    id                UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    recipient_id      VARCHAR(255)             NOT NULL,
    channel           VARCHAR(10)              NOT NULL,
    payload           TEXT                     NOT NULL,
    status            VARCHAR(10)              NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    sent_at           TIMESTAMP WITH TIME ZONE,
    error_description TEXT,
    is_read           BOOLEAN                  NOT NULL DEFAULT FALSE,
    archived          BOOLEAN                  NOT NULL DEFAULT FALSE
);