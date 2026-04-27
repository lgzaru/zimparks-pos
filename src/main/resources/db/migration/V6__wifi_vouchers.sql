-- Add WiFi product fields to products table
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS wifi_product   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS voucher_prefix VARCHAR(10);

-- WiFi vouchers generated per transaction item
CREATE TABLE IF NOT EXISTS wifi_vouchers (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)    NOT NULL UNIQUE,
    tx_ref          VARCHAR(30)    NOT NULL,
    product_code    VARCHAR(20)    NOT NULL,
    product_descr   VARCHAR(200)   NOT NULL,
    amount          NUMERIC(10, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wifi_vouchers_tx_ref ON wifi_vouchers (tx_ref);
