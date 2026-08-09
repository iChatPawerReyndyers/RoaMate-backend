package com.roamate.sync.apply;

import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;

/**
 * Turns one queued, previously-offline event back into a real entity
 * mutation. Registered as a Spring bean; SyncService discovers all of
 * them and dispatches by EventType. Without an implementation for a given
 * type, that type's events are still persisted to the audit log by
 * SyncService but never actually applied - see SyncService's dispatch
 * loop for how an unmatched type is handled.
 */
public interface EventApplier {
    EventType supportedType();

    /** Throws on any failure; SyncService catches this per-event so one bad event doesn't fail the whole batch. */
    void apply(EventLogEntity event) throws Exception;
}
