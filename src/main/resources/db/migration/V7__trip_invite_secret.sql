-- TRIP-01: "React Native app generates a unique 6-character code AND an
-- offline-scannable QR code embedded with trip cryptographic keys." The
-- 6-character invite_code alone was doing double duty for both the typed
-- and the scanned paths, which means anyone who sees/guesses a 6-character
-- code (33^6 ~= 1.3B combinations, from a small unambiguous alphabet) can
-- join exactly like a QR scan would. This adds a separate, much higher-
-- entropy secret that only ever travels inside the QR payload, never shown
-- as text - so scanning becomes a meaningfully stronger join path than
-- typing, matching what the spec actually describes.
--
-- Backfilled from two concatenated uuid_generate_v4() calls (32 hex chars
-- each, no dashes) for any pre-existing trips, then locked to NOT NULL.
ALTER TABLE trips ADD COLUMN invite_secret VARCHAR(64);

UPDATE trips
SET invite_secret = replace(uuid_generate_v4()::text, '-', '') || replace(uuid_generate_v4()::text, '-', '')
WHERE invite_secret IS NULL;

ALTER TABLE trips ALTER COLUMN invite_secret SET NOT NULL;