package com.roamate.sync;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/events")
    public List<EventLogEntity> pushEvents(@RequestBody List<EventLogEntity> events) {
        return syncService.ingestBatch(events);
    }

    @GetMapping("/trips/{tripId}/events")
    public List<EventLogEntity> pullEvents(@PathVariable UUID tripId,
                                            @RequestParam(required = false) Instant since) {
        return syncService.fetchSince(tripId, since != null ? since : Instant.EPOCH);
    }
}
