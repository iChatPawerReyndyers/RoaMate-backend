package com.roamate.checklist.dto;

/**
 * CHK-05: moves an item to a different category. For PACKING items this is a
 * PackingItemCategory name (CLOTHING, ELECTRONICS, TOILETRIES, GEAR); for
 * GROCERY / CUSTOM items it's the free-text store section (Produce, Dairy...).
 * null (or blank) clears it, which the app shows as "Other".
 */
public record SetChecklistItemCategoryRequest(String category) {}