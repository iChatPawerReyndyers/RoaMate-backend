-- ITIN-03: mark each pinned stop as REQUIRED, OPTIONAL, or TENTATIVE so the
-- itinerary can visually distinguish must-do stops from maybes. Mirrors the
-- existing checklist_items.priority column added in V3.
ALTER TABLE destinations ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'REQUIRED';
