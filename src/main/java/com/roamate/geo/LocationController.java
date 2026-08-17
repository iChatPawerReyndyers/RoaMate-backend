package com.roamate.geo;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo")
public class LocationController {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final GeoRepository geoRepository;
    private final SilentPushService silentPushService;

    public LocationController(GeoRepository geoRepository, SilentPushService silentPushService) {
        this.geoRepository = geoRepository;
        this.silentPushService = silentPushService;
    }

    /** GEO-02/03: opening the map triggers a silent-push fan-out and waits (bounded) for fresh fixes - see SilentPushService. */
    @GetMapping("/trips/{tripId}/locations")
    public List<MemberLocation> getTripLocations(@PathVariable UUID tripId,
                                                  @AuthenticationPrincipal(expression = "subject") String requestingUserId) {
        return silentPushService.refreshTripLocations(tripId, requestingUserId);
    }

    /** Called by the device's background push handler after a silent-push GPS fix (GEO-04 marks it non-stale, fresh). */
    @PostMapping("/trips/{tripId}/locations")
    public MemberLocation reportLocation(@PathVariable UUID tripId,
                                          @RequestParam String userId,
                                          @RequestParam double lat,
                                          @RequestParam double lng) {
        MemberLocation location = geoRepository.findByTripIdAndUserId(tripId, userId)
                .orElseGet(MemberLocation::new);
        location.setTripId(tripId);
        location.setUserId(userId);
        location.setCoordinates(GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(lng, lat)));
        location.setCapturedAt(Instant.now());
        location.setStale(false);
        MemberLocation saved = geoRepository.save(location);

        // Must happen after save() above, not before: refreshTripLocations()
        // (woken by this call) reads member_locations back out once every
        // in-flight future resolves, so the write needs to already be
        // durably committed by the time that read runs.
        silentPushService.completeLocation(tripId, userId, lat, lng);

        return saved;
    }
}
