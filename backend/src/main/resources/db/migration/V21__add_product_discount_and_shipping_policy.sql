ALTER TABLE products
    ADD COLUMN IF NOT EXISTS discount_start_date DATE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS discount_end_date DATE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS free_shipping BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS shipping_fee NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS estimated_delivery_days INTEGER NOT NULL DEFAULT 3;

UPDATE products
SET discount_start_date = CURRENT_DATE,
    discount_end_date = CURRENT_DATE + 30
WHERE discount_percentage > 0;

ALTER TABLE products
    ADD CONSTRAINT chk_products_discount_period
        CHECK (
            (
                discount_percentage = 0
                AND discount_start_date IS NULL
                AND discount_end_date IS NULL
            )
            OR
            (
                discount_percentage > 0
                AND discount_start_date IS NOT NULL
                AND discount_end_date IS NOT NULL
                AND discount_start_date <= discount_end_date
            )
        ),
    ADD CONSTRAINT chk_products_shipping_fee
        CHECK (
            (free_shipping = TRUE AND shipping_fee = 0)
            OR
            (free_shipping = FALSE AND shipping_fee > 0)
        ),
    ADD CONSTRAINT chk_products_estimated_delivery_days
        CHECK (estimated_delivery_days BETWEEN 1 AND 30);
