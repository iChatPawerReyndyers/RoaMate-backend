-- RoaMate v1.5.1 initial schema
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE trips (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    invite_code VARCHAR(6) NOT NULL UNIQUE,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE trip_members (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (trip_id, user_id)
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    total_amount_cents BIGINT NOT NULL,
    expense_date TIMESTAMPTZ NOT NULL,
    category VARCHAR(100),
    created_by_user_id VARCHAR(255) NOT NULL,
    flagged_duplicate BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE expense_payments (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    source VARCHAR(20) NOT NULL,
    payer_user_id VARCHAR(255),
    amount_paid_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE expense_participants (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    fair_share_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE kitty_deposits (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    depositor_user_id VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    deposited_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE member_locations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    coordinates geometry(Point,4326) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    stale BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (trip_id, user_id)
);

CREATE TABLE beacon_alerts (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    raised_by_user_id VARCHAR(255) NOT NULL,
    coordinates geometry(Point,4326) NOT NULL,
    raised_at TIMESTAMPTZ NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE destinations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    coordinates geometry(Point,4326),
    assigned_day DATE,
    sort_order INT NOT NULL DEFAULT 0,
    notes VARCHAR(2000),
    attachment_urls TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE location_notes (
    id UUID PRIMARY KEY,
    destination_id UUID NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
    author_user_id VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE activity_sessions (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    step_count INT,
    distance_meters DOUBLE PRECISION,
    elevation_gain_meters DOUBLE PRECISION,
    relative_depth_meters DOUBLE PRECISION,
    destination_id UUID,
    started_at TIMESTAMPTZ NOT NULL,
    last_batch_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE checklist_items (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    category VARCHAR(20) NOT NULL,
    label VARCHAR(255) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT false,
    assigned_to_user_id VARCHAR(255),
    converted_expense_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE event_log (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    origin_device_id VARCHAR(255) NOT NULL,
    origin_user_id VARCHAR(255) NOT NULL,
    client_timestamp TIMESTAMPTZ NOT NULL,
    payload_json TEXT NOT NULL,
    applied BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);
