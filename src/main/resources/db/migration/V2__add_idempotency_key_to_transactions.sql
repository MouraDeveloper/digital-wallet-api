ALTER TABLE transactions
    ADD COLUMN "idempotency_key" VARCHAR(255) UNIQUE;