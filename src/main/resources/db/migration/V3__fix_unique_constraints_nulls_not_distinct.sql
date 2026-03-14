-- ===================================================================
-- V3: Recreate unique constraints with NULLS NOT DISTINCT
--
-- PostgreSQL's default UNIQUE constraint treats NULL as distinct from
-- every other value (including another NULL), so two rows that are
-- identical except for a NULL column never violate the constraint.
-- This caused re-importing the same CSV file to insert duplicates for
-- any row whose project_name (rents) or trans_id/actual_area (sales)
-- happened to be NULL.
--
-- NULLS NOT DISTINCT (PostgreSQL 15+) makes NULLs compare as equal
-- within a unique index, so re-imports of the same data are correctly
-- detected as conflicts and skipped via ON CONFLICT DO NOTHING.
-- ===================================================================

-- Sales ------------------------------------------------------------
ALTER TABLE sale_transactions DROP CONSTRAINT IF EXISTS uq_sale_trans_id_area;
ALTER TABLE sale_transactions
    ADD CONSTRAINT uq_sale_trans_id_area
        UNIQUE NULLS NOT DISTINCT (trans_id, actual_area);

-- Rents ------------------------------------------------------------
ALTER TABLE rent_transactions DROP CONSTRAINT IF EXISTS uq_rent_dedup;
ALTER TABLE rent_transactions
    ADD CONSTRAINT uq_rent_dedup
        UNIQUE NULLS NOT DISTINCT (contract_date, start_date, end_date,
                                   area_name, actual_area, annual_amount,
                                   project_name);
