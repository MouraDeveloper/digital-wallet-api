CREATE TABLE users
(
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE wallets
(
    id         UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID           NOT NULL UNIQUE,
    balance    NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_wallets PRIMARY KEY (id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE transactions
(
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    type                  VARCHAR(10)    NOT NULL,
    source_wallet_id      UUID,
    destination_wallet_id UUID           NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_tx_source FOREIGN KEY (source_wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_tx_destination FOREIGN KEY (destination_wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_tx_type CHECK (
        (type = 'DEPOSIT' AND source_wallet_id IS NULL) OR
        (type = 'TRANSFER' AND source_wallet_id IS NOT NULL AND source_wallet_id <> destination_wallet_id)
        )
);

CREATE INDEX idx_tx_source_created ON transactions (source_wallet_id, created_at DESC);
CREATE INDEX idx_tx_destination_created ON transactions (destination_wallet_id, created_at DESC);
