CREATE TABLE product_return_requests (
    return_request_id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    seller_response VARCHAR(500),
    payment_refund_id BIGINT,
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    CONSTRAINT fk_return_request_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_return_request_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_return_request_payment_refund
        FOREIGN KEY (payment_refund_id) REFERENCES payment_refunds (id),
    CONSTRAINT ck_return_request_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_return_request_status
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_return_request_order_item
    ON product_return_requests (order_item_id);

CREATE INDEX idx_return_request_member
    ON product_return_requests (member_id, requested_at DESC);

CREATE INDEX idx_return_request_status
    ON product_return_requests (status, requested_at);
