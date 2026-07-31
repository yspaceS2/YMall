DO $$
BEGIN
    IF to_regclass('orders') IS NOT NULL THEN
        ALTER TABLE orders
            DROP CONSTRAINT IF EXISTS orders_status_check;
        ALTER TABLE orders
            ADD CONSTRAINT orders_status_check
                CHECK (status IN (
                    'PENDING_PAYMENT',
                    'PAID',
                    'PAYMENT_FAILED',
                    'CANCELED',
                    'PARTIALLY_REFUNDED',
                    'REFUNDED',
                    'PREPARING',
                    'SHIPPED',
                    'DELIVERED'
                ));
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('payments') IS NOT NULL THEN
        ALTER TABLE payments
            DROP CONSTRAINT IF EXISTS payments_order_status_check;
        ALTER TABLE payments
            ADD CONSTRAINT payments_order_status_check
                CHECK (order_status IN (
                    'PENDING_PAYMENT',
                    'PAID',
                    'PAYMENT_FAILED',
                    'CANCELED',
                    'PARTIALLY_REFUNDED',
                    'REFUNDED',
                    'PREPARING',
                    'SHIPPED',
                    'DELIVERED'
                ));
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('order_outbox_events') IS NOT NULL THEN
        ALTER TABLE order_outbox_events
            DROP CONSTRAINT IF EXISTS order_outbox_events_event_type_check;
        ALTER TABLE order_outbox_events
            ADD CONSTRAINT order_outbox_events_event_type_check
                CHECK (event_type IN (
                    'ORDER_CREATED',
                    'PAYMENT_COMPLETED',
                    'PAYMENT_FAILED',
                    'ORDER_CANCELED',
                    'ORDER_PREPARING',
                    'ORDER_SHIPPED',
                    'ORDER_DELIVERED',
                    'REFUND_COMPLETED'
                ));
    END IF;
END
$$;
