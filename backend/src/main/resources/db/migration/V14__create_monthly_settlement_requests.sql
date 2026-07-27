CREATE TABLE settlement_requests (
    id BIGSERIAL PRIMARY KEY,
    seller_profile_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    gross_amount NUMERIC(38, 2) NOT NULL,
    fee_amount NUMERIC(38, 2) NOT NULL,
    settlement_amount NUMERIC(38, 2) NOT NULL,
    rejection_reason VARCHAR(500),
    reviewed_by_member_id BIGINT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    mock_payment_reference VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_settlement_request_seller_period
        UNIQUE (seller_profile_id, period_start),
    CONSTRAINT fk_settlement_request_seller_profile
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles(id),
    CONSTRAINT fk_settlement_request_reviewer
        FOREIGN KEY (reviewed_by_member_id) REFERENCES members(id)
);

ALTER TABLE settlement_ledger_entries
    ADD COLUMN settlement_request_id BIGINT,
    ADD CONSTRAINT fk_settlement_ledger_request
        FOREIGN KEY (settlement_request_id) REFERENCES settlement_requests(id);

CREATE INDEX idx_settlement_request_status_created
    ON settlement_requests (status, created_at DESC);

CREATE INDEX idx_settlement_ledger_request
    ON settlement_ledger_entries (settlement_request_id);

CREATE TABLE settlement_request_histories (
    id BIGSERIAL PRIMARY KEY,
    settlement_request_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    actor_member_id BIGINT NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_settlement_history_request
        FOREIGN KEY (settlement_request_id) REFERENCES settlement_requests(id),
    CONSTRAINT fk_settlement_history_actor
        FOREIGN KEY (actor_member_id) REFERENCES members(id)
);

CREATE INDEX idx_settlement_history_request_created
    ON settlement_request_histories (settlement_request_id, created_at);
