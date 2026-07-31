DO $$
BEGIN
    IF to_regclass('notifications') IS NOT NULL THEN
        ALTER TABLE notifications
            DROP CONSTRAINT IF EXISTS notifications_type_check;
        ALTER TABLE notifications
            ADD CONSTRAINT notifications_type_check
                CHECK (type IN (
                    'ORDER_CREATED',
                    'PAYMENT_COMPLETED',
                    'PAYMENT_FAILED',
                    'ORDER_CANCELED',
                    'ORDER_PREPARING',
                    'ORDER_SHIPPED',
                    'ORDER_DELIVERED',
                    'RETURN_REQUESTED',
                    'RETURN_APPROVED',
                    'RETURN_REJECTED',
                    'PRODUCT_QUESTION_CREATED',
                    'PRODUCT_QUESTION_ANSWERED'
                ));
    END IF;
END
$$;
