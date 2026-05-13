SELECT 1 AS connection_test;

SELECT count() AS user_count FROM test_users;

SELECT count() AS order_count FROM test_orders;

SELECT u.name, count(o.id) AS order_count
FROM test_users u
LEFT JOIN test_orders o ON u.id = o.user_id
GROUP BY u.name
ORDER BY order_count DESC
LIMIT 10;
