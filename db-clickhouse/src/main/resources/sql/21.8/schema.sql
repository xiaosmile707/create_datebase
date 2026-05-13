CREATE TABLE IF NOT EXISTS test_users (
    id UInt32,
    name String,
    email Nullable(String),
    created_at DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY id;

CREATE TABLE IF NOT EXISTS test_orders (
    id UInt32,
    user_id UInt32,
    amount Decimal(10, 2),
    status String DEFAULT 'pending',
    created_at DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY id;
