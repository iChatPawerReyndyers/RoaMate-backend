-- Geospatial (GIST) indexes for radius/proximity queries (GEO-01..04) and
-- destination map rendering. B-tree indexes for the hot lookup paths used
-- by the sync and settlement engines.

CREATE INDEX idx_member_locations_coordinates ON member_locations USING GIST (coordinates);
CREATE INDEX idx_beacon_alerts_coordinates ON beacon_alerts USING GIST (coordinates);
CREATE INDEX idx_destinations_coordinates ON destinations USING GIST (coordinates);

CREATE INDEX idx_expenses_trip_id ON expenses (trip_id) WHERE deleted = false;
CREATE INDEX idx_expenses_dup_lookup ON expenses (trip_id, description, expense_date) WHERE deleted = false;
CREATE INDEX idx_expense_payments_expense_id ON expense_payments (expense_id);
CREATE INDEX idx_expense_participants_expense_id ON expense_participants (expense_id);
CREATE INDEX idx_kitty_deposits_trip_id ON kitty_deposits (trip_id) WHERE deleted = false;

CREATE INDEX idx_event_log_trip_ts ON event_log (trip_id, client_timestamp);

CREATE INDEX idx_destinations_trip_day ON destinations (trip_id, assigned_day, sort_order) WHERE deleted = false;
CREATE INDEX idx_activity_sessions_trip_user ON activity_sessions (trip_id, user_id);
CREATE INDEX idx_checklist_items_trip_category ON checklist_items (trip_id, category) WHERE deleted = false;
