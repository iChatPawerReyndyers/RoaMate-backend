-- ACT-05: null until "Finish activity at this stop" is tapped on the
-- Activity Dashboard, so the Pinned Location Card can gate its metrics
-- section on an explicit "done here" signal instead of on ActivitySession
-- existence (sessions get created mid-activity, well before the traveler
-- is actually finished with the stop).
ALTER TABLE destinations ADD COLUMN activity_completed_at TIMESTAMPTZ NULL;
