-- Online payments table: tracks mobile money (EcoCash/OneMoney/TeleCash) payment requests
-- A record is created when a USSD prompt is pushed to a customer's phone.
-- The txRef column is populated once Paynow confirms payment and the transaction is created.
CREATE TABLE IF NOT EXISTS online_payments (
    paynow_ref        VARCHAR(50)      PRIMARY KEY,
    cell              VARCHAR(20),
    amount            DECIMAL(10, 2),
    currency          VARCHAR(5),
    status            VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    poll_url          TEXT,
    payment_ref       VARCHAR(100),
    description       VARCHAR(255),
    tx_ref            VARCHAR(30),
    tx_payload        TEXT,
    operator_username VARCHAR(100),
    error_message     VARCHAR(500),
    created_at        TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);
