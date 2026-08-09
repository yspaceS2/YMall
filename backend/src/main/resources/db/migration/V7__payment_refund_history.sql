DO $$
BEGIN
    IF to_regclass('order_items') IS NOT NULL THEN
        ALTER TABLE order_items
            ADD COLUMN IF NOT EXISTS refunded_quantity INTEGER NOT NULL DEFAULT 0;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('payments') IS NOT NULL
        AND to_regclass('orders') IS NOT NULL
        AND to_regclass('order_items') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS payment_refunds (
            id BIGSERIAL PRIMARY KEY,
            payment_id BIGINT NOT NULL REFERENCES payments(id),
            order_id BIGINT NOT NULL REFERENCES orders(id),
            idempotency_key VARCHAR(100) NOT NULL,
            type VARCHAR(20) NOT NULL,
            status VARCHAR(20) NOT NULL,
            amount NUMERIC(38, 2) NOT NULL,
            reason VARCHAR(200) NOT NULL,
            requested_by_member_id BIGINT NOT NULL,
            requested_by_role VARCHAR(30) NOT NULL,
            provider_status VARCHAR(30),
            provider_balance_amount NUMERIC(38, 2),
            failure_code VARCHAR(100),
            failure_message VARCHAR(500),
            created_at TIMESTAMP NOT NULL,
            processed_at TIMESTAMP,
            CONSTRAINT uk_payment_refunds_order_idempotency_key
                UNIQUE (order_id, idempotency_key)
        );

        CREATE TABLE IF NOT EXISTS payment_refund_items (
            id BIGSERIAL PRIMARY KEY,
            refund_id BIGINT NOT NULL REFERENCES payment_refunds(id) ON DELETE CASCADE,
            order_item_id BIGINT NOT NULL REFERENCES order_items(id),
            quantity INTEGER NOT NULL CHECK (quantity > 0),
            amount NUMERIC(38, 2) NOT NULL CHECK (amount > 0)
        );

        CREATE INDEX IF NOT EXISTS idx_payment_refunds_order_id
            ON payment_refunds (order_id, created_at DESC);

        CREATE INDEX IF NOT EXISTS idx_payment_refund_items_order_item_id
            ON payment_refund_items (order_item_id);
    END IF;
END
$$;
