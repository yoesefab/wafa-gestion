-- LOCAL DEVELOPMENT ONLY. This is intentionally outside Flyway's migration location.
-- Removes application data while preserving the schema and Flyway history.
TRUNCATE TABLE app_users, categories, customers, suppliers RESTART IDENTITY CASCADE;
ALTER SEQUENCE sales_order_number_seq RESTART WITH 1;
ALTER SEQUENCE purchase_order_number_seq RESTART WITH 1;
