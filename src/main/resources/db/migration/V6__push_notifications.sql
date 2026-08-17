-- GEO-01: the "Share My Location" toggle was previously pure local React
-- state on the client, never sent to the server at all - so it had zero
-- effect on what GET /trips/{id}/locations actually returned. GEO-03 needs
-- a real server-side flag to know which members to silent-push in the
-- first place ("members with active toggles"), so this promotes it to a
-- real per-membership column. Defaults to false per spec ("Defaults to OFF").
ALTER TABLE trip_members ADD COLUMN location_sharing_enabled BOOLEAN NOT NULL DEFAULT false;

-- GEO-02/03: a device's push token, needed to actually address a silent
-- push at it. One user can have multiple devices/platforms registered;
-- token itself is unique (a token belongs to exactly one app install at a
-- time - re-registering the same token for a different user, e.g. after a
-- logout/login on a shared device, should move it rather than duplicate).
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    token VARCHAR(4096) NOT NULL UNIQUE,
    platform VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
