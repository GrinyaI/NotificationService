CREATE TABLE notification_outbox
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID         NOT NULL REFERENCES notifications (id),
    topic           VARCHAR(255) NOT NULL,
    message_key     VARCHAR(255) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    published_at    TIMESTAMP WITH TIME ZONE,
    last_error      TEXT
);

CREATE INDEX ix_notification_outbox_status_created_at
    ON notification_outbox (status, created_at);

CREATE INDEX ix_notification_outbox_notification_id
    ON notification_outbox (notification_id);
