-- ITIN-06: optional planned stay length for a pinned stop, in whole minutes
-- (e.g. 150 = 2 hrs 30 mins). Integer minutes rather than fractional hours so
-- 0.25 / 0.5 / 0.75 hr steps never pick up float rounding error. NULL means
-- "no planned duration" - every existing row stays NULL.
ALTER TABLE destinations ADD COLUMN planned_duration_minutes INT NULL;