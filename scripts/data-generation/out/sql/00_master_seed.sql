-- Generated from world.yaml at 2025-09-05T08:40:35.273871Z
-- World: demo-world-tr
-- Master Data: Products
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1001, 'Sunscreen SPF50 200ml', 'Personal Care', 'şişe', 20.0, 50.0, 119.9);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1002, 'Air Conditioner 12000 BTU', 'Appliances', 'adet', 2.0, 10.0, 8499.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1003, 'Ski Board All-Mountain', 'Sports', 'adet', 2.0, 8.0, 3999.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1004, 'Thermo Flask 500ml', 'Home & Kitchen', 'adet', 10.0, 30.0, 249.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1005, 'Dates (Medjool) 1kg', 'Grocery', 'kg', 15.0, 40.0, 149.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1006, 'Assorted Chocolates 250g', 'Confectionery', 'paket', 20.0, 60.0, 39.9);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1007, 'Eau de Parfum 50ml', 'Fragrance', 'şişe', 5.0, 20.0, 899.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1008, 'Potato Chips 50g', 'Snacks', 'paket', 80.0, 200.0, 12.5);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1009, 'Energy Drink 250ml Can', 'Beverages', 'şişe', 60.0, 150.0, 25.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1010, 'School Notebook A4 80 pages', 'Stationery', 'adet', 40.0, 120.0, 18.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1011, 'Turkish Flag 70x105 cm', 'Occasions', 'adet', 10.0, 30.0, 75.0);
INSERT INTO products(product_id, product_name, category, unit_of_measure, safety_stock, reorder_point, current_price) VALUES (1012, 'Black Tea 1kg', 'Grocery', 'paket', 30.0, 80.0, 89.0);

-- Master Data: Suppliers
INSERT INTO suppliers(supplier_id, supplier_name, email, phone, city) VALUES (11, 'Anadolu Wholesale', 'contact@anadoluwholesale.example', '+90 212 555 0001', 'Istanbul');
INSERT INTO suppliers(supplier_id, supplier_name, email, phone, city) VALUES (12, 'Marmara Supply', 'sales@marmarasupply.example', '+90 216 555 0002', 'Istanbul');
INSERT INTO suppliers(supplier_id, supplier_name, email, phone, city) VALUES (13, 'Aegean Foods', 'hello@aegeanfoods.example', '+90 232 555 0003', 'Izmir');
INSERT INTO suppliers(supplier_id, supplier_name, email, phone, city) VALUES (14, 'Uludag Beverages', 'ops@uludagbev.example', '+90 224 555 0004', 'Bursa');

-- Master Data: Customers
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (501, 'Blue Mart Electronics', 'CORPORATE', 'orders@bluemart.example', '+90 532 123 4501', 'Ankara');
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (502, 'Green Valley Grocery', 'SME', 'sales@greenvalley.example', '+90 532 123 4502', 'Istanbul');
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (503, 'Yildiz Home Goods', 'SME', 'hello@yildizhome.example', '+90 532 123 4503', 'Izmir');
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (504, 'TechHub Ankara', 'ENTERPRISE', 'purchasing@techhub.example', '+90 532 123 4504', 'Ankara');
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (505, 'Retail Coop Istanbul', 'CORPORATE', 'coop@istanbulretail.example', '+90 532 123 4505', 'Istanbul');
INSERT INTO customers(customer_id, customer_name, customer_segment, email, phone, city) VALUES (506, 'Anadolu Corner Store', 'INDIVIDUAL', 'owner@anadolucorner.example', '+90 532 123 4506', 'Bursa');

-- Links: Product-Suppliers
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1001, 11, 24.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1001, 12, 24.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1002, 12, 2.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1002, 11, 2.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1003, 12, 2.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1003, 11, 2.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1004, 11, 12.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1004, 13, 12.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1005, 13, 20.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1005, 11, 20.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1006, 13, 48.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1006, 12, 48.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1007, 11, 6.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1007, 12, 6.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1008, 13, 120.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1008, 11, 120.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1009, 14, 96.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1009, 11, 96.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1010, 12, 100.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1010, 11, 100.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1011, 11, 20.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1011, 12, 20.0, FALSE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1012, 13, 40.0, TRUE, TRUE);
INSERT INTO product_suppliers(product_id, supplier_id, min_order_quantity, is_preferred, active) VALUES (1012, 11, 40.0, FALSE, TRUE);
