CREATE INDEX IF NOT EXISTS idx_orders_pending_expiration
    ON orders (created_at, id)
    WHERE status = 'PENDING_PAYMENT'
      AND inventory_reserved = TRUE;
