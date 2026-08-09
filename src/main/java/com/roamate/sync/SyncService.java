package com.roamate.sync;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Merges incoming batches of events in client-timestamp order (not
 * server-arrival order), so two devices that were offline concurrently get
 * a deterministic, chronologically-correct replay rather than whichever
 * uploaded first winning. Actual per-entity mutation (creating the Expense,
 * ChecklistItem, etc.) is delegated to each module's own service - this
 * class is purely the ordering + persistence layer for the log itself.
 */
@Service
public class SyncService {

    private final EventLogRepository eventLogRepository;

    public SyncService(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    @Transactional
    public List<EventLogEntity> ingestBatch(List<EventLogEntity> incoming) {
        incoming.sort(Comparator.comparing(EventLogEntity::getClientTimestamp));
        return eventLogRepository.saveAll(incoming);
    }

    public List<EventLogEntity> fetchSince(UUID tripId, Instant since) {
        return eventLogRepository.findByTripIdAndClientTimestampAfterOrderByClientTimestampAsc(tripId, since);
    }
}
