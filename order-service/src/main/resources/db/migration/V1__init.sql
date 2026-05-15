CREATE TABLE IF NOT EXISTS orders (
    id         BIGSERIAL    NOT NULL,
    status     VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    deleted_at TIMESTAMP    NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_deleted_at ON orders (deleted_at);

CREATE TABLE IF NOT EXISTS order_items (
    order_id     BIGINT       NOT NULL,
    inventory_id BIGINT       NOT NULL,
    product_id   VARCHAR(255) NOT NULL,
    sku_code     VARCHAR(255) NOT NULL,
    price        INT          NOT NULL,
    quantity     INT          NOT NULL,
    status       VARCHAR(255) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);

CREATE TABLE IF NOT EXISTS order_inventory_request_outbox (
    id              BIGSERIAL    NOT NULL,
    status          VARCHAR(255) NOT NULL,
    retry           INT          NOT NULL,
    next_start_from TIMESTAMP    NOT NULL,
    order_id        BIGINT       NOT NULL,
    inventory_id    BIGINT       NOT NULL,
    amount          INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    deleted_at      TIMESTAMP    NULL,
    CONSTRAINT pk_order_inventory_request_outbox PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_start
    ON order_inventory_request_outbox (status, next_start_from);
CREATE INDEX IF NOT EXISTS idx_outbox_order_id
    ON order_inventory_request_outbox (order_id);
CREATE INDEX IF NOT EXISTS idx_outbox_deleted_at
    ON order_inventory_request_outbox (deleted_at);
