CREATE TABLE push_subscriptions
(
    id           UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    recipient_id VARCHAR(255)             NOT NULL,
    fcm_token    TEXT                     NOT NULL,
    platform     VARCHAR(30)              NOT NULL DEFAULT 'WEB',
    active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_push_subscriptions_fcm_token
    ON push_subscriptions (fcm_token);

CREATE INDEX ix_push_subscriptions_recipient_active
    ON push_subscriptions (recipient_id, active);
