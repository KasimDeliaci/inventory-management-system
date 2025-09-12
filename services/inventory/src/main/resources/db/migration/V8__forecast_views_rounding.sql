-- ======================================================================
-- V8__forecast_views_rounding.sql  (PostgreSQL)
-- Purpose: Create forecasting schema and views with tidy numeric formatting
--  - Create inv_forecast schema for forecasting-related objects
--  - Round percentage-like fields to 2 decimals for friendlier consumption
--    • inv_forecast.v_product_day_sales.offer_active_share → 2 decimals
--    • inv_forecast.v_product_day_promo.promo_pct          → 2 decimals
-- Notes:
--  - Uses CREATE OR REPLACE VIEW; safe to run after V8
--  - Values remain NUMERIC; callers may still format as needed
-- ======================================================================

-- Create the forecasting schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS inv_forecast;

-- Create v_product_day_sales with rounded offer_active_share
CREATE OR REPLACE VIEW inv_forecast.v_product_day_sales AS
SELECT
  so.order_date::date                                           AS date,
  soi.product_id                                                AS product_id,
  SUM(soi.quantity)                                             AS sales_units,
  CASE WHEN SUM(soi.quantity) > 0
       THEN ROUND(
              (SUM(CASE WHEN so.customer_special_offer_id IS NOT NULL THEN soi.quantity ELSE 0 END)::numeric
               / SUM(soi.quantity)::numeric),
              2
            )::numeric(5,2)
       ELSE 0.00::numeric(5,2)
  END                                                           AS offer_active_share
FROM sales_orders so
JOIN sales_order_items soi
  ON soi.sales_order_id = so.sales_order_id
WHERE so.status = 'DELIVERED'
GROUP BY so.order_date::date, soi.product_id;

COMMENT ON VIEW inv_forecast.v_product_day_sales IS
  'Daily product sales by order_date for DELIVERED orders; offer_active_share rounded to 2 decimals.';

-- Recreate v_product_day_promo with rounded promo_pct
CREATE OR REPLACE VIEW inv_forecast.v_product_day_promo AS
SELECT
  g.d::date                                                     AS date,
  cp.product_id                                                 AS product_id,
  ROUND(
    MAX(
      CASE
        WHEN c.campaign_type = 'DISCOUNT' THEN c.discount_percentage
        WHEN c.campaign_type = 'BXGY_SAME_PRODUCT' AND (c.buy_qty + c.get_qty) > 0
          THEN (100.0 * c.get_qty::numeric) / (c.buy_qty + c.get_qty)
        ELSE 0::numeric
      END
    ), 2
  )::numeric(5,2)                                               AS promo_pct
FROM campaign_products cp
JOIN campaigns c
  ON c.campaign_id = cp.campaign_id
CROSS JOIN LATERAL generate_series(c.start_date, c.end_date, interval '1 day') AS g(d)
GROUP BY g.d::date, cp.product_id;

COMMENT ON VIEW inv_forecast.v_product_day_promo IS
  'Daily effective promotion percent per product (DISCOUNT/BXGY); promo_pct rounded to 2 decimals.';

-- ------------------------------------------------------------
-- Day-level customer offer aggregates (exogenous, product-agnostic)
--   • purpose: provide per-day signals about customer offers for forecasting
--   • columns:
--       date                :: date
--       active_offers_count :: integer
--       offer_avg_pct       :: numeric(5,2)
--       offer_max_pct       :: numeric(5,2)
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW inv_forecast.v_day_offer_stats AS
WITH bounds AS (
  SELECT MIN(start_date) AS min_d, MAX(end_date) AS max_d
  FROM customer_special_offers
), days AS (
  SELECT g.d::date AS date
  FROM bounds b
  CROSS JOIN LATERAL generate_series(b.min_d, b.max_d, interval '1 day') AS g(d)
)
SELECT
  d.date,
  COUNT(o.special_offer_id)                                             AS active_offers_count,
  COALESCE(ROUND(AVG(o.percent_off)::numeric, 2), 0.00)::numeric(5,2)   AS offer_avg_pct,
  COALESCE(ROUND(MAX(o.percent_off)::numeric, 2), 0.00)::numeric(5,2)   AS offer_max_pct
FROM days d
LEFT JOIN customer_special_offers o
  ON o.start_date <= d.date AND o.end_date >= d.date
GROUP BY d.date
ORDER BY d.date;

COMMENT ON VIEW inv_forecast.v_day_offer_stats IS
  'Per-day aggregates of customer special offers (count/avg/max percent_off). Use as exogenous features for forecasting.';
  