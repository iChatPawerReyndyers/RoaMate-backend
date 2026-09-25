package com.roamate.checklist.dto;

/**
 * CHK-05: who is in charge of a SHARED checklist item. null (or blank)
 * clears the assignment. Exact-set semantics - this body is the whole
 * change, so nothing else on the item can be overwritten by it.
 */
public record AssignChecklistItemRequest(String assignedToUserId) {}