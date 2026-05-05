INSERT INTO test_users (name, email) VALUES
('Alice', 'alice@example.com'),
('Bob', 'bob@example.com'),
('Charlie', 'charlie@example.com');

INSERT INTO test_orders (user_id, amount, status) VALUES
(1, 99.99, 'completed'),
(1, 49.50, 'pending'),
(2, 150.00, 'completed'),
(3, 29.90, 'cancelled'),
(3, 199.00, 'completed');
