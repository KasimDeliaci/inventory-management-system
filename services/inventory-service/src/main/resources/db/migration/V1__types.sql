-- ENUMs used across the schema
DROP TYPE IF EXISTS movement_source;
DROP TYPE IF EXISTS movement_kind;
DROP TYPE IF EXISTS customer_segment;

CREATE TYPE movement_source AS ENUM ('SALES_ORDER','PURCHASE_ORDER','ADJUSTMENT');
CREATE TYPE movement_kind   AS ENUM ('purchase','sale','return','adjustment');
CREATE TYPE customer_segment AS ENUM ('INDIVIDUAL','SME','CORPORATE','ENTERPRISE','OTHER');
