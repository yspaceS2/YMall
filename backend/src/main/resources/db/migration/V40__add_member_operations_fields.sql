ALTER TABLE members
    ADD COLUMN access_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN last_login_at TIMESTAMP,
    ADD COLUMN restriction_reason VARCHAR(500),
    ADD COLUMN restricted_at TIMESTAMP,
    ADD COLUMN restricted_by BIGINT REFERENCES members (id);

ALTER TABLE members
    ADD CONSTRAINT members_access_status_check
        CHECK (access_status IN ('ACTIVE', 'RESTRICTED')),
    ADD CONSTRAINT members_restriction_state_check CHECK (
        (access_status = 'ACTIVE'
            AND restriction_reason IS NULL
            AND restricted_at IS NULL
            AND restricted_by IS NULL)
        OR (access_status = 'RESTRICTED'
            AND restriction_reason IS NOT NULL
            AND restricted_at IS NOT NULL
            AND restricted_by IS NOT NULL)
    );

CREATE INDEX idx_members_admin_operations
    ON members (access_status, role, admin_grade, created_at DESC, id DESC);

ALTER TABLE admin_audit_logs
    DROP CONSTRAINT admin_audit_logs_action_check;

ALTER TABLE admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_action_check CHECK (action IN (
        'ADMIN_ROLE_CHANGED', 'MEMBER_RESTRICTION_CHANGED', 'MEMBER_SESSIONS_REVOKED',
        'SELLER_APPLICATION_REVIEWED', 'PRODUCT_REVIEWED', 'REFUND_PROCESSED',
        'SETTLEMENT_APPROVED', 'CATEGORY_POLICY_CHANGED'
    ));
