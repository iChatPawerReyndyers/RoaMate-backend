package com.roamate.checklist;

/**
 * CHK-01: PERSONAL items live only on the owning member's device/account.
 * SHARED items sync to the whole group with ONE person in charge of
 * bringing it (see ChecklistItem.assignedToUserId).
 * CHK-06: EVERYONE items also sync to the whole group, but nobody is
 * assigned - each trip member is expected to bring/buy their own, so
 * assigning a single person to one doesn't make sense (see
 * ChecklistService.assign, which rejects it same as PERSONAL).
 */
public enum ChecklistVisibility {
    PERSONAL,
    SHARED,
    EVERYONE
}