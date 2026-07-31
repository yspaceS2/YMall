DO $$
BEGIN
    IF to_regclass('product_return_requests') IS NOT NULL THEN
        ALTER TABLE product_return_requests
            DROP CONSTRAINT IF EXISTS fk_return_request_payment_refund;
        ALTER TABLE product_return_requests
            ADD CONSTRAINT fk_return_request_payment_refund
                FOREIGN KEY (payment_refund_id) REFERENCES payment_refunds (id);

        ALTER TABLE product_return_requests
            DROP CONSTRAINT IF EXISTS ck_return_request_status;
        ALTER TABLE product_return_requests
            ADD CONSTRAINT ck_return_request_status
                CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED'));
    END IF;
END
$$;
