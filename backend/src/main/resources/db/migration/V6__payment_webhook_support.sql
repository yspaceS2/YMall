DO $$
BEGIN
    IF to_regclass('payments') IS NOT NULL THEN
        ALTER TABLE payments
            ADD COLUMN IF NOT EXISTS provider_status VARCHAR(30);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    transmission_id VARCHAR(200) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payment_key VARCHAR(200) NOT NULL,
    payment_order_id VARCHAR(64) NOT NULL,
    requested_status VARCHAR(30) NOT NULL,
    verified_status VARCHAR(30) NOT NULL,
    result VARCHAR(30) NOT NULL,
    event_created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payment_webhook_events_transmission_id UNIQUE (transmission_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_payment_key
    ON payment_webhook_events (payment_key);
