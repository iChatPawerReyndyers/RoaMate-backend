package com.roamate.geo;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo/beacons")
public class BeaconController {

    private final BeaconAlertRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public BeaconController(BeaconAlertRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    /** GEO-05: raising a beacon is urgent by definition - push it to online members immediately. */
    @PostMapping
    public BeaconAlert raise(@RequestBody BeaconAlert alert) {
        BeaconAlert saved = repository.save(alert);
        messagingTemplate.convertAndSend("/topic/trips/" + saved.getTripId() + "/beacons", saved);
        return saved;
    }

    @GetMapping("/trips/{tripId}/active")
    public List<BeaconAlert> listActive(@PathVariable UUID tripId) {
        return repository.findByTripIdAndAcknowledgedFalse(tripId);
    }

    @PostMapping("/{id}/acknowledge")
    public BeaconAlert acknowledge(@PathVariable UUID id) {
        BeaconAlert alert = repository.findById(id).orElseThrow();
        alert.setAcknowledged(true);
        return repository.save(alert);
    }
}
