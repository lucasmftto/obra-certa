-- Remove planned amount/date, keep only actual values renamed to amount/paid_at
UPDATE expense SET actual_amount = 0 WHERE actual_amount IS NULL;

ALTER TABLE expense
    DROP COLUMN IF EXISTS planned_amount,
    DROP COLUMN IF EXISTS planned_date;

ALTER TABLE expense
    RENAME COLUMN actual_amount TO amount;

ALTER TABLE expense
    RENAME COLUMN actual_date TO paid_at;

ALTER TABLE expense
    ALTER COLUMN amount SET NOT NULL;
