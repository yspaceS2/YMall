ALTER TABLE settlement_requests
    DROP CONSTRAINT IF EXISTS uq_settlement_request_seller_period;

ALTER TABLE settlement_requests
    ALTER COLUMN period_start DROP NOT NULL,
    ALTER COLUMN period_end DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_settlement_request_seller_created
    ON settlement_requests (seller_profile_id, created_at DESC);
