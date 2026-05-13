INSERT INTO test_users (id, name, email) VALUES
(1, 'Alice', 'alice@example.com'),
(2, 'Bob', 'bob@example.com'),
(3, 'Charlie', 'charlie@example.com');

INSERT INTO test_orders (id, user_id, amount, status) VALUES
(1, 1, 99.99, 'completed'),
(2, 1, 49.50, 'pending'),
(3, 2, 150.00, 'completed'),
(4, 3, 29.90, 'cancelled'),
(5, 3, 199.00, 'completed');
