\set ON_ERROR_STOP on

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM members) THEN
        RAISE EXCEPTION 'At least one synthetic benchmark member is required.';
    END IF;

    IF to_regclass('uk_orders_payment_order_id') IS NULL THEN
        RAISE EXCEPTION 'The canonical payment_order_id index is required.';
    END IF;
END
$$;

\echo 'Duplicate payment_order_id index'
BEGIN;

CREATE UNIQUE INDEX ymall77_benchmark_duplicate_payment_order_id
    ON orders (payment_order_id);

EXPLAIN (ANALYZE, BUFFERS, WAL, SUMMARY)
INSERT INTO orders (
    member_id,
    idempotency_key,
    payment_order_id,
    status,
    total_amount,
    inventory_reserved,
    created_at,
    updated_at
)
SELECT
    benchmark_member.id,
    format('ymall77-duplicate-%s-%s', txid_current(), sequence_number),
    format('YMALL77-DUP-%s-%s', txid_current(), sequence_number),
    'PENDING_PAYMENT',
    0.00,
    TRUE,
    clock_timestamp(),
    clock_timestamp()
FROM (
    SELECT id
    FROM members
    ORDER BY id
    LIMIT 1
) benchmark_member
CROSS JOIN generate_series(1, 1000) sequence_number;

ROLLBACK;

\echo 'Single payment_order_id index'
BEGIN;

EXPLAIN (ANALYZE, BUFFERS, WAL, SUMMARY)
INSERT INTO orders (
    member_id,
    idempotency_key,
    payment_order_id,
    status,
    total_amount,
    inventory_reserved,
    created_at,
    updated_at
)
SELECT
    benchmark_member.id,
    format('ymall77-single-%s-%s', txid_current(), sequence_number),
    format('YMALL77-ONE-%s-%s', txid_current(), sequence_number),
    'PENDING_PAYMENT',
    0.00,
    TRUE,
    clock_timestamp(),
    clock_timestamp()
FROM (
    SELECT id
    FROM members
    ORDER BY id
    LIMIT 1
) benchmark_member
CROSS JOIN generate_series(1, 1000) sequence_number;

ROLLBACK;
