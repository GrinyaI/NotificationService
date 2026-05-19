UPDATE notifications
SET idempotency_key = 'legacy-' || id::text
WHERE idempotency_key IS NULL;

ALTER TABLE notifications
    ALTER COLUMN idempotency_key SET NOT NULL;

DROP INDEX IF EXISTS ux_notifications_idempotency_channel;

CREATE UNIQUE INDEX ux_notifications_idempotency_channel
    ON notifications (idempotency_key, channel);

CREATE UNIQUE INDEX ux_notification_outbox_notification_id
    ON notification_outbox (notification_id);
