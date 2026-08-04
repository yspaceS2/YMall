ALTER TABLE seller_applications
    DROP CONSTRAINT IF EXISTS ck_seller_application_status,
    DROP CONSTRAINT IF EXISTS seller_applications_status_check;

ALTER TABLE seller_applications
    ADD CONSTRAINT seller_applications_status_check
        CHECK (status IN ('PENDING', 'NEEDS_REVISION', 'APPROVED', 'REJECTED'));
