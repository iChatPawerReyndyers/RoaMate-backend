package com.roamate.geo;

import com.roamate.geo.dto.RaiseBeaconRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo/beacons")
public class BeaconController {

    private final BeaconService beaconService;
    private final BeaconAlertRepository repository;

    public BeaconController(BeaconService beaconService, BeaconAlertRepository repository) {
        this.beaconService = beaconService;
        this.repository = repository;
    }

    /** GEO-05: raising a beacon is urgent by definition - push it to online members immediately. */
    @PostMapping
    public BeaconAlert raise(@RequestBody RaiseBeaconRequest request) {
        return beaconService.raise(request);
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
