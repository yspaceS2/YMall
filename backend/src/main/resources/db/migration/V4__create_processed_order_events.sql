CREATE TABLE processed_order_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    result VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_order_events_order_result_id
    ON processed_order_events (order_id, result, id DESC);
