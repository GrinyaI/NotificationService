ALTER TABLE notifications
    ALTER COLUMN recipient_id DROP NOT NULL,
    ADD COLUMN audience_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    ADD COLUMN audience_target VARCHAR(255),
    ADD COLUMN destination VARCHAR(255),
    ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN idempotency_key VARCHAR(128);

UPDATE notifications
SET audience_target = recipient_id,
    destination = recipient_id
WHERE audience_target IS NULL;

CREATE UNIQUE INDEX ux_notifications_idempotency_channel
    ON notifications (idempotency_key, channel)
    WHERE idempotency_key IS NOT NULL;
