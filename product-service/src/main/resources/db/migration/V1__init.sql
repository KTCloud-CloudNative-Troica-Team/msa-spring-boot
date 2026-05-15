CREATE TABLE IF NOT EXISTS products (
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    price       NUMERIC      NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT pk_products PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON products (deleted_at);
