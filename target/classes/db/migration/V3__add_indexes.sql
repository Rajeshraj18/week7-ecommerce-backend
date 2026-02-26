-- V3__add_indexes.sql

SET search_path TO project;

-- Index for User Email
CREATE INDEX idx_user_email ON users(email);

-- Index for Product Name and Category
CREATE INDEX idx_product_name ON products(name);
CREATE INDEX idx_product_category ON products(category_id);

-- Index for Order User and Status
CREATE INDEX idx_order_user ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at);

-- Index for Order Items Order ID
CREATE INDEX idx_order_item_order ON order_items(order_id);
CREATE INDEX idx_order_item_product ON order_items(product_id);

-- Index for Payments Transaction ID
CREATE INDEX idx_payment_transaction ON payments(transaction_id);
