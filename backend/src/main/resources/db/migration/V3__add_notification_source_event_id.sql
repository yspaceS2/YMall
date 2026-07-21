ALTER TABLE notifications
    ADD COLUMN source_event_id UUID;

CREATE UNIQUE INDEX uk_notifications_source_event_id
    ON notifications (source_event_id);
