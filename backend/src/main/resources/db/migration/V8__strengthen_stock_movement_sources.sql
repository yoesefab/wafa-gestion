ALTER TABLE sales_order_items
    ADD CONSTRAINT uk_sales_order_items_id_product UNIQUE (id, product_id);

ALTER TABLE purchase_order_items
    ADD CONSTRAINT uk_purchase_order_items_id_product UNIQUE (id, product_id);

ALTER TABLE stock_movements DROP CONSTRAINT fk_stock_movements_sales_item;
ALTER TABLE stock_movements DROP CONSTRAINT fk_stock_movements_purchase_item;

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_sales_item_product
        FOREIGN KEY (sales_order_item_id, product_id)
        REFERENCES sales_order_items (id, product_id);

ALTER TABLE stock_movements
    ADD CONSTRAINT fk_stock_movements_purchase_item_product
        FOREIGN KEY (purchase_order_item_id, product_id)
        REFERENCES purchase_order_items (id, product_id);

ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_source_consistency CHECK (
        NOT (sales_order_item_id IS NOT NULL AND purchase_order_item_id IS NOT NULL)
        AND (
            sales_order_item_id IS NULL
            OR (movement_type = 'STOCK_OUT' AND quantity_delta < 0)
        )
        AND (
            purchase_order_item_id IS NULL
            OR (movement_type = 'STOCK_IN' AND quantity_delta > 0)
        )
    );
