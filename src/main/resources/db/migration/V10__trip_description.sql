-- TRIP-01: optional free-text description shown in the trip info modal
-- alongside the trip name and member list.
ALTER TABLE trips ADD COLUMN description TEXT;
