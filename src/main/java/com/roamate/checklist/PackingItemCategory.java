package com.roamate.checklist;

/** CHK-01: sub-category for PACKING items specifically - unused/null for GROCERY items, which use storeCategory (a free-text field) instead since grocery store sections don't fit a fixed enum the way packing categories do. */
public enum PackingItemCategory {
    CLOTHING,
    ELECTRONICS,
    TOILETRIES,
    GEAR
}