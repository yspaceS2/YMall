DO $$
BEGIN
    IF to_regclass('products') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS review_summaries (
            id BIGSERIAL PRIMARY KEY,
            product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
            summary_json TEXT NOT NULL,
            source_review_count BIGINT NOT NULL,
            source_updated_at TIMESTAMP,
            model_version VARCHAR(200) NOT NULL,
            generated_at TIMESTAMP NOT NULL,
            CONSTRAINT uk_review_summaries_product_id UNIQUE (product_id)
        );
    END IF;
END
$$;
