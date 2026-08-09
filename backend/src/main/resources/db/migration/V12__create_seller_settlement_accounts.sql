CREATE TABLE seller_settlement_accounts (
    id BIGSERIAL PRIMARY KEY,
    seller_profile_id BIGINT NOT NULL UNIQUE,
    bank_code VARCHAR(3) NOT NULL,
    account_holder_ciphertext TEXT NOT NULL,
    account_number_ciphertext TEXT NOT NULL,
    account_number_last4 VARCHAR(4) NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_seller_settlement_account_profile
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles(id)
);
