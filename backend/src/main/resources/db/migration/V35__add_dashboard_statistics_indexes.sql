CREATE INDEX IF NOT EXISTS idx_orders_dashboard_created_at
    ON orders (created_at, id);

CREATE INDEX IF NOT EXISTS idx_order_items_dashboard_order_product
    ON order_items (order_id, product_id);

CREATE INDEX IF NOT EXISTS idx_order_items_dashboard_product_order
    ON order_items (product_id, order_id);

CREATE INDEX IF NOT EXISTS idx_products_dashboard_seller
    ON products (seller_profile_id, id);

CREATE INDEX IF NOT EXISTS idx_members_dashboard_created_at
    ON members (created_at);

CREATE INDEX IF NOT EXISTS idx_seller_profiles_dashboard_created_at
    ON seller_profiles (created_at);
