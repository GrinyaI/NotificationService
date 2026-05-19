ALTER TABLE notifications
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE notification_outbox
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX ix_notifications_recipient_channel_status_created_at
    ON notifications (recipient_id, channel, status, created_at DESC)
    WHERE archived = FALSE;

CREATE INDEX ix_notifications_recipient_created_at
    ON notifications (recipient_id, created_at DESC)
    WHERE archived = FALSE;

CREATE INDEX ix_notifications_archive_created_at
    ON notifications (archived, created_at);
