-- ENUMs used across the schema
DROP TYPE IF EXISTS movement_source;
DROP TYPE IF EXISTS movement_kind;

CREATE TYPE movement_source AS ENUM ('SALES_ORDER','PURCHASE_ORDER','ADJUSTMENT');
CREATE TYPE movement_kind   AS ENUM ('purchase','sale','return','adjustment');
