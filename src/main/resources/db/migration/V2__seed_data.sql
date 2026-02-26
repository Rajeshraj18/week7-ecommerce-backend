-- V2__seed_data.sql

SET search_path TO project;

-- Insert 150 Users (Mixed Admins and Regular users)
INSERT INTO users (email, password, name, role)
SELECT 
    'user' || i || '@example.com',
    'encoded_password_here',
    'User ' || i,
    CASE WHEN i % 10 = 0 THEN 'ADMIN' ELSE 'USER' END
FROM generate_series(1, 150) AS i;

-- Insert 50 Categories (Hierarchical subset)
INSERT INTO categories (name, description, parent_category_id)
SELECT 
    'Category ' || i,
    'Description for Category ' || i,
    CASE WHEN i > 10 THEN (i % 10) + 1 ELSE NULL END
FROM generate_series(1, 50) AS i;

-- Insert 200 Products
INSERT INTO products (name, description, price, stock, category_id, image_url, is_active)
SELECT 
    'Product ' || i,
    'Description for Premium Product ' || i,
    (random() * 500 + 10)::numeric(10,2),
    Floor(random() * 1000 + 10)::int,
    (i % 50) + 1,
    'image_' || i || '.jpg',
    true
FROM generate_series(1, 200) AS i;

-- Insert 250 Orders
INSERT INTO orders (order_number, user_id, total_amount, status, shipping_address)
SELECT 
    'ORD-2024-' || LPAD(i::text, 6, '0'),
    (i % 150) + 1,
    (random() * 1000 + 50)::numeric(10,2),
    CASE 
        WHEN i % 5 = 0 THEN 'CANCELLED'
        WHEN i % 5 = 1 THEN 'PENDING'
        WHEN i % 5 = 2 THEN 'PROCESSING'
        WHEN i % 5 = 3 THEN 'SHIPPED'
        ELSE 'COMPLETED'
    END,
    '123 Random Address ' || i || ', Test City'
FROM generate_series(1, 250) AS i;

-- Insert 500 Order Items (roughly 2 items per order)
INSERT INTO order_items (order_id, product_id, quantity, price)
SELECT 
    (i % 250) + 1,
    (i % 200) + 1,
    Floor(random() * 5 + 1)::int,
    (random() * 300 + 10)::numeric(10,2)
FROM generate_series(1, 500) AS i;

-- Insert 250 Payments (Linking precisely to orders 1 -> 250)
INSERT INTO payments (order_id, amount, method, status, transaction_id)
SELECT 
    i,
    (random() * 1000 + 50)::numeric(10,2),
    CASE WHEN i % 2 = 0 THEN 'CREDIT_CARD' ELSE 'PAYPAL' END,
    CASE WHEN i % 5 = 0 THEN 'FAILED' ELSE 'SUCCESS' END,
    'TXN-' || LPAD(i::text, 10, '0')
FROM generate_series(1, 250) AS i;
