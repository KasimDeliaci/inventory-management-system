-- =====================================================================
-- V1__types.sql  (PostgreSQL 17.6)
-- Purpose: Shared ENUMs for inventory domain (PoC-friendly)
-- Notes:
--   - Safe in early dev: drops existing types if present.
--   - After schema stabilizes, avoid dropping ENUMs in new migrations.
-- =====================================================================

-- Drop if present (early dev convenience)
DROP TYPE IF EXISTS purchase_order_status;
DROP TYPE IF EXISTS movement_kind;
DROP TYPE IF EXISTS movement_source;
DROP TYPE IF EXISTS customer_segment;
DROP TYPE IF EXISTS sales_order_status;

-- -------------------------------------------------------
-- Customer segmentation
-- -------------------------------------------------------
CREATE TYPE customer_segment AS ENUM (
  'INDIVIDUAL','SME','CORPORATE','ENTERPRISE','OTHER'
);

-- -------------------------------------------------------
-- Purchase order lifecycle (PoC: 4 states)
-- -------------------------------------------------------
CREATE TYPE purchase_order_status AS ENUM (
  'PLACED',
  'IN_TRANSIT',
  'RECEIVED',
  'CANCELLED'
);

-- -------------------------------------------------------
-- Sales order lifecycle (PoC: 5 states)
-- -------------------------------------------------------
CREATE TYPE sales_order_status AS ENUM (
  'PENDING',
  'ALLOCATED',
  'IN_TRANSIT',
  'DELIVERED',
  'CANCELLED'
);

-- -------------------------------------------------------
-- Where the movement originated (business workflow)
-- -------------------------------------------------------
CREATE TYPE movement_source AS ENUM (
  'PURCHASE_ORDER',   -- movement created from a PO flow
  'SALES_ORDER',      -- movement created from a SO flow
  'ADJUSTMENT'        -- manual/system inventory adjustment
);

-- -------------------------------------------------------
-- What physically happened to stock (and direction)
-- Minimal PoC set (no returning mechanism planned for PoC)
-- -------------------------------------------------------
CREATE TYPE movement_kind AS ENUM (
  'PURCHASE_RECEIPT',   -- + stock from supplier
  'SALE_SHIPMENT',      -- - stock to customer
  'ADJUSTMENT_IN',      -- + manual/system correction
  'ADJUSTMENT_OUT'      -- - manual/system correction
);