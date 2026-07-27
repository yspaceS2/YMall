CREATE TABLE settlement_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    seller_profile_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    payment_refund_id BIGINT,
    entry_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gross_amount NUMERIC(38, 2) NOT NULL,
    fee_amount NUMERIC(38, 2) NOT NULL,
    settlement_amount NUMERIC(38, 2) NOT NULL,
    source_event_id UUID NOT NULL,
    source_key VARCHAR(120) NOT NULL UNIQUE,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_settlement_ledger_seller_profile
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles(id),
    CONSTRAINT fk_settlement_ledger_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_settlement_ledger_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_settlement_ledger_payment_refund
        FOREIGN KEY (payment_refund_id) REFERENCES payment_refunds(id)
);

CREATE INDEX idx_settlement_ledger_seller_status_occurred
    ON settlement_ledger_entries (seller_profile_id, status, occurred_at DESC);

CREATE INDEX idx_settlement_ledger_order
    ON settlement_ledger_entries (order_id);
