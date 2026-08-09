package com.roamate.sync;

/** Every offline-capable entity type gets a corresponding event type for the sync log. */
public enum EventType {
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    KITTY_DEPOSIT_CREATED,
    LOCATION_NOTE_CREATED,
    DESTINATION_PINNED,
    ACTIVITY_SESSION_RECORDED,
    CHECKLIST_ITEM_TOGGLED,
    BEACON_ALERT_RAISED
}
