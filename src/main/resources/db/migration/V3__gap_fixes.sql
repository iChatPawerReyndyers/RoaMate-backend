-- Closes gaps found in the post-build spec audit.

-- ACT-02/section 8.1: unified activity record needs duration + min/max altitude,
-- not just a single elevation-gain figure.
ALTER TABLE activity_sessions ADD COLUMN duration_seconds BIGINT;
ALTER TABLE activity_sessions ADD COLUMN max_altitude_meters DOUBLE PRECISION;
ALTER TABLE activity_sessions ADD COLUMN min_altitude_meters DOUBLE PRECISION;

-- GEO-05: the spec's beacon statuses ("Arrived Safely", "Need Assistance at
-- Pin X") were missing a field entirely - beacons only had raise/acknowledge.
ALTER TABLE beacon_alerts ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'NEED_ASSISTANCE';
ALTER TABLE beacon_alerts ADD COLUMN message VARCHAR(500);
ALTER TABLE beacon_alerts ADD COLUMN destination_id UUID;

-- ITIN-02: operating hours and target budget allocation per stop were missing.
ALTER TABLE destinations ADD COLUMN operating_hours VARCHAR(255);
ALTER TABLE destinations ADD COLUMN target_budget_cents BIGINT;

-- CHK-01: Personal-vs-Shared item visibility was missing entirely.
ALTER TABLE checklist_items ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'SHARED';
ALTER TABLE checklist_items ADD COLUMN owner_user_id VARCHAR(255);

-- CHK-03: grocery-specific fields (quantity, priority, store category, buyer)
-- were missing from the shared checklist_items table.
ALTER TABLE checklist_items ADD COLUMN quantity INT;
ALTER TABLE checklist_items ADD COLUMN priority VARCHAR(10);
ALTER TABLE checklist_items ADD COLUMN store_category VARCHAR(100);

-- CHK-02: custom, user-saved templates (beyond the single hardcoded packing
-- template) need their own table.
CREATE TABLE checklist_templates (
    id UUID PRIMARY KEY,
    trip_id UUID,
    owner_user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(20) NOT NULL,
    items_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);
