CREATE TABLE IF NOT EXISTS seller_applications (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    store_name VARCHAR(100) NOT NULL,
    business_number VARCHAR(20) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_seller_application_member
        FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_seller_application_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES members(id),
    CONSTRAINT ck_seller_application_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_seller_applications_status_created_at
    ON seller_applications (status, created_at);

CREATE INDEX IF NOT EXISTS idx_seller_applications_business_number
    ON seller_applications (business_number);
