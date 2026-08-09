-- ITIN-02: "address" was named in the spec's field list but never actually
-- added to the destinations table (operating_hours and target_budget_cents
-- were added in V3, address was missed).
ALTER TABLE destinations ADD COLUMN address VARCHAR(500);

-- ACT-04: destinations.summary lookups filter activity_sessions by
-- destination_id; this was previously unindexed since findByDestinationId
-- was defined on the repository but never queried from a controller.
CREATE INDEX idx_activity_sessions_destination_id ON activity_sessions (destination_id);
