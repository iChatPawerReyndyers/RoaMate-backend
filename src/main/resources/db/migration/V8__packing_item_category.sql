-- CHK-01: adds the missing packing-item sub-category (Clothing/Electronics/
-- Toiletries/Gear) the spec calls for. Nullable and unused for GROCERY rows.
ALTER TABLE checklist_items ADD COLUMN packing_item_category VARCHAR(20);