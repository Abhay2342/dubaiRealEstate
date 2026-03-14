-- contract_id is never populated from the actual CSV data source
-- (no CONTRACT_NUM / CONTRACT_ID column exists), so the column is always NULL
-- and provides no business value.
ALTER TABLE rent_transactions DROP COLUMN IF EXISTS contract_id;
