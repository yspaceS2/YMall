CREATE TABLE product_revisions (
    product_revision_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(100),
    thumbnail_url TEXT,
    status VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_product_revisions_pending
    ON product_revisions(product_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_product_revisions_status_created
    ON product_revisions(status, created_at DESC);

CREATE TABLE product_revision_images (
    product_revision_image_id BIGSERIAL PRIMARY KEY,
    product_revision_id BIGINT NOT NULL REFERENCES product_revisions(product_revision_id)
        ON DELETE CASCADE,
    original_url TEXT,
    image_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE TABLE product_revision_detail_images (
    product_revision_detail_image_id BIGSERIAL PRIMARY KEY,
    product_revision_id BIGINT NOT NULL REFERENCES product_revisions(product_revision_id)
        ON DELETE CASCADE,
    original_url TEXT,
    image_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL
);
