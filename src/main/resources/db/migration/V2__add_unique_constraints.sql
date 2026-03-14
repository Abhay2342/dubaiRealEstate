-- ===================================================================
-- V2: Add unique constraints to prevent duplicate imports
-- ===================================================================

-- Sales: (trans_id, actual_area) is the natural dedup key.
-- The same TRANSACTION_NUMBER can appear for multiple units in a
-- portfolio mortgage, each with a different actual_area.
-- NULLs in either column are excluded from the constraint by PostgreSQL
-- semantics (NULL != NULL), so incomplete rows won't block each other.
ALTER TABLE sale_transactions
    ADD CONSTRAINT uq_sale_trans_id_area UNIQUE (trans_id, actual_area);

-- Rents: The DLD rents CSV has no contract-number column, so we use a
-- composite of seven fields that together identify a unique contract.
-- project_name is included because two different properties in the same
-- area can legitimately share the same date/size/price (e.g. two studios
-- in two different buildings with identical rent registered at the same time).
ALTER TABLE rent_transactions
    ADD CONSTRAINT uq_rent_dedup
        UNIQUE (contract_date, start_date, end_date, area_name, actual_area, annual_amount, project_name);
