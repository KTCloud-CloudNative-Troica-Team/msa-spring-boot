CREATE TABLE IF NOT EXISTS inventories (
    id         BIGSERIAL    NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    sku_code   VARCHAR(255) NOT NULL,
    quantity   INT          NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    deleted_at TIMESTAMP    NULL,
    CONSTRAINT pk_inventories PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_inventories_product_sku ON inventories (product_id, sku_code);
CREATE INDEX IF NOT EXISTS idx_inventories_deleted_at ON inventories (deleted_at);

CREATE TABLE IF NOT EXISTS inventory_events (
    id             BIGSERIAL    NOT NULL,
    inventory_id   BIGINT       NOT NULL,
    amount         INT          NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    process_status VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    deleted_at     TIMESTAMP    NULL,
    CONSTRAINT pk_inventory_events PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_events_inventory_id ON inventory_events (inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_events_process_status ON inventory_events (process_status);
CREATE INDEX IF NOT EXISTS idx_inventory_events_deleted_at ON inventory_events (deleted_at);
