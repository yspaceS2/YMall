CREATE TABLE IF NOT EXISTS product_questions (
    question_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_questions_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_questions_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_product_questions_status
        CHECK (status IN ('WAITING', 'ANSWERED'))
);

CREATE INDEX IF NOT EXISTS idx_product_questions_product_created
    ON product_questions (product_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_questions_status_created
    ON product_questions (status, created_at DESC);

CREATE TABLE IF NOT EXISTS product_question_answers (
    answer_id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE,
    seller_profile_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_question_answers_question
        FOREIGN KEY (question_id) REFERENCES product_questions (question_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_product_question_answers_seller
        FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles (id)
);
