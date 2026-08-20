CREATE INDEX idx_sales_orders_status_confirmed_at
    ON sales_orders (status, confirmed_at);

CREATE INDEX idx_products_active_stock_threshold
    ON products (active, current_stock, minimum_stock);
