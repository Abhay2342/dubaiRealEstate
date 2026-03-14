-- ===================================================================
-- Dubai Real Estate – Initial Schema
-- V1__create_tables.sql
-- ===================================================================

-- Enable trigram extension for fast ILIKE search (requires superuser on first run)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS sale_transactions (
    id                BIGSERIAL       PRIMARY KEY,
    trans_id          VARCHAR(100),
    trans_date        DATE,
    trans_value       NUMERIC(20, 2),
    area_name         VARCHAR(200),
    project_name      VARCHAR(200),
    usage             VARCHAR(100),
    registration_type VARCHAR(100),
    property_type     VARCHAR(100),
    rooms             VARCHAR(50),
    actual_area       NUMERIC(15, 2),
    meter_sale_price  NUMERIC(20, 2),
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sale_area       ON sale_transactions (area_name);
CREATE INDEX IF NOT EXISTS idx_sale_date       ON sale_transactions (trans_date);
CREATE INDEX IF NOT EXISTS idx_sale_usage      ON sale_transactions (usage);
CREATE INDEX IF NOT EXISTS idx_sale_project    ON sale_transactions (project_name);

-- Full-text search index for area + project
CREATE INDEX IF NOT EXISTS idx_sale_area_trgm    ON sale_transactions USING gin (area_name    gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sale_project_trgm ON sale_transactions USING gin (project_name gin_trgm_ops);

-- ===================================================================

CREATE TABLE IF NOT EXISTS rent_transactions (
    id            BIGSERIAL       PRIMARY KEY,
    contract_id   VARCHAR(100),
    contract_date DATE,
    annual_amount NUMERIC(20, 2),
    start_date    DATE,
    end_date      DATE,
    area_name     VARCHAR(200),
    project_name  VARCHAR(200),
    usage         VARCHAR(100),
    property_type VARCHAR(100),
    rooms         VARCHAR(50),
    actual_area   NUMERIC(15, 2),
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rent_area       ON rent_transactions (area_name);
CREATE INDEX IF NOT EXISTS idx_rent_date       ON rent_transactions (contract_date);
CREATE INDEX IF NOT EXISTS idx_rent_usage      ON rent_transactions (usage);
CREATE INDEX IF NOT EXISTS idx_rent_project    ON rent_transactions (project_name);

CREATE INDEX IF NOT EXISTS idx_rent_area_trgm    ON rent_transactions USING gin (area_name    gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_rent_project_trgm ON rent_transactions USING gin (project_name gin_trgm_ops);
