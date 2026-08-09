package com.roamate.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventLogRepository extends JpaRepository<EventLogEntity, UUID> {
    List<EventLogEntity> findByTripIdAndClientTimestampAfterOrderByClientTimestampAsc(UUID tripId, Instant since);
}
