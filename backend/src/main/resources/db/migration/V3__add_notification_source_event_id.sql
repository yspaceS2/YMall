DO $$
BEGIN
    IF to_regclass('notifications') IS NOT NULL THEN
        ALTER TABLE notifications
            ADD COLUMN IF NOT EXISTS source_event_id UUID;

        CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_source_event_id
            ON notifications (source_event_id);
    END IF;
END
$$;
