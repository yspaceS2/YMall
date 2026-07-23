DO $$
BEGIN
    IF to_regclass('orders') IS NOT NULL THEN
        ALTER TABLE orders
            ADD COLUMN IF NOT EXISTS payment_order_id VARCHAR(64);

        UPDATE orders
        SET payment_order_id = 'YMALL-LEGACY-' || id
        WHERE payment_order_id IS NULL;

        ALTER TABLE orders
            ALTER COLUMN payment_order_id SET NOT NULL;

        CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_payment_order_id
            ON orders (payment_order_id);

        ALTER TABLE orders
            ADD COLUMN IF NOT EXISTS inventory_reserved BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    IF to_regclass('payments') IS NOT NULL THEN
        ALTER TABLE payments
            ADD COLUMN IF NOT EXISTS payment_key VARCHAR(200),
            ADD COLUMN IF NOT EXISTS payment_order_id VARCHAR(64),
            ADD COLUMN IF NOT EXISTS requested_amount NUMERIC(38, 2),
            ADD COLUMN IF NOT EXISTS approved_amount NUMERIC(38, 2),
            ADD COLUMN IF NOT EXISTS method VARCHAR(50),
            ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE,
            ADD COLUMN IF NOT EXISTS failure_code VARCHAR(100);

        CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_payment_key
            ON payments (payment_key)
            WHERE payment_key IS NOT NULL;
    END IF;
END
$$;
