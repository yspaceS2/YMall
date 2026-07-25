DO $$
DECLARE
    duplicate_constraint RECORD;
BEGIN
    IF to_regclass('orders') IS NULL THEN
        RETURN;
    END IF;

    CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_payment_order_id
        ON orders (payment_order_id);

    FOR duplicate_constraint IN
        SELECT constraint_info.conname
        FROM pg_constraint constraint_info
        JOIN LATERAL (
            SELECT array_agg(attribute_info.attname ORDER BY key_info.ordinality) AS column_names
            FROM unnest(constraint_info.conkey) WITH ORDINALITY AS key_info(attnum, ordinality)
            JOIN pg_attribute attribute_info
                ON attribute_info.attrelid = constraint_info.conrelid
                AND attribute_info.attnum = key_info.attnum
        ) constraint_columns ON TRUE
        WHERE constraint_info.conrelid = 'orders'::regclass
            AND constraint_info.contype = 'u'
            AND constraint_info.conname <> 'uk_orders_payment_order_id'
            AND constraint_columns.column_names = ARRAY['payment_order_id']::name[]
    LOOP
        EXECUTE format(
            'ALTER TABLE orders DROP CONSTRAINT %I',
            duplicate_constraint.conname
        );
    END LOOP;
END
$$;
