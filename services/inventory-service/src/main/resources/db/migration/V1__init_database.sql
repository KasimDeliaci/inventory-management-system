CREATE TABLE IF NOT EXISTS product (
	product_id VARCHAR(255) NOT NULL PRIMARY KEY,
	product_name VARCHAR(255) NOT NULL,
	description TEXT,
	category VARCHAR(255),
	unit_of_measure VARCHAR(255),
	safety_stock DOUBLE PRECISION,
	reorder_point DOUBLE PRECISION,
	current_price DOUBLE PRECISION
);

CREATE SEQUENCE IF NOT EXISTS product_seq INCREMENT BY 50;