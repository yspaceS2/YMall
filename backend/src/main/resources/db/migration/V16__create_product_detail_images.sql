CREATE TABLE product_detail_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    original_url TEXT,
    image_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_detail_images_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT ck_product_detail_images_sort_order
        CHECK (sort_order >= 0)
);

CREATE INDEX idx_product_detail_images_product_sort_order
    ON product_detail_images (product_id, sort_order, id);
