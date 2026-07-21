CREATE TABLE IF NOT EXISTS order_outbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    order_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_outbox_publishable
    ON order_outbox_events (status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_order_outbox_published_cleanup
    ON order_outbox_events (status, published_at);
