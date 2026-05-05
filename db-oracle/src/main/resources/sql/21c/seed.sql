INSERT INTO test_users (name, email) VALUES ('Alice', 'alice@example.com');
INSERT INTO test_users (name, email) VALUES ('Bob', 'bob@example.com');
INSERT INTO test_users (name, email) VALUES ('Charlie', 'charlie@example.com');

INSERT INTO test_orders (user_id, amount, status) VALUES (1, 99.99, 'completed');
INSERT INTO test_orders (user_id, amount, status) VALUES (1, 49.50, 'pending');
INSERT INTO test_orders (user_id, amount, status) VALUES (2, 150.00, 'completed');
INSERT INTO test_orders (user_id, amount, status) VALUES (3, 29.90, 'cancelled');
INSERT INTO test_orders (user_id, amount, status) VALUES (3, 199.00, 'completed');
