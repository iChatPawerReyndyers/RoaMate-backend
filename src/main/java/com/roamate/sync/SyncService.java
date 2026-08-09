package com.roamate.sync;

import com.roamate.sync.apply.EventApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merges incoming batches of events in client-timestamp order (not
 * server-arrival order), so two devices that were offline concurrently get
 * a deterministic, chronologically-correct replay rather than whichever
 * uploaded first winning. Actual per-entity mutation (creating the Expense,
 * ChecklistItem, etc.) is delegated to each module's own service via a
 * registered EventApplier (see the com.roamate.sync.apply package) - this
 * class owns ordering, dispatch, and persistence of the log itself.
 *
 * Not every EventType has a registered applier yet (e.g. EXPENSE_CREATED,
 * KITTY_DEPOSIT_CREATED - those flows don't currently queue offline events
 * at all, they only call their REST endpoint directly). An event with no
 * matching applier is still persisted to the log for audit purposes but is
 * never actually applied; `applied` stays false on that row so it's easy
 * to find via a query if that gap needs closing later.
 */
@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final EventLogRepository eventLogRepository;
    private final Map<EventType, EventApplier> appliersByType;

    public SyncService(EventLogRepository eventLogRepository, List<EventApplier> appliers) {
        this.eventLogRepository = eventLogRepository;
        this.appliersByType = appliers.stream()
                .collect(Collectors.toMap(EventApplier::supportedType, applier -> applier));
    }

    @Transactional
    public List<EventLogEntity> ingestBatch(List<EventLogEntity> incoming) {
        incoming.sort(Comparator.comparing(EventLogEntity::getClientTimestamp));

        for (EventLogEntity event : incoming) {
            EventApplier applier = appliersByType.get(event.getEventType());
            if (applier == null) {
                log.debug("No EventApplier registered for {}; logged but not replayed.", event.getEventType());
                continue;
            }
            try {
                applier.apply(event);
                event.setApplied(true);
            } catch (Exception ex) {
                // One malformed/conflicting event shouldn't fail the whole
                // batch - it stays in the log with applied=false for
                // investigation rather than blocking every other device's sync.
                log.warn("Failed to apply {} event {}: {}", event.getEventType(), event.getId(), ex.getMessage());
            }
        }

        return eventLogRepository.saveAll(incoming);
    }

    public List<EventLogEntity> fetchSince(UUID tripId, Instant since) {
        return eventLogRepository.findByTripIdAndClientTimestampAfterOrderByClientTimestampAsc(tripId, since);
    }
}
