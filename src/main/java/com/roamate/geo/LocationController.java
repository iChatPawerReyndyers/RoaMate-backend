package com.roamate.geo;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geo")
public class LocationController {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final GeoRepository geoRepository;

    public LocationController(GeoRepository geoRepository) {
        this.geoRepository = geoRepository;
    }

    @GetMapping("/trips/{tripId}/locations")
    public List<MemberLocation> getTripLocations(@PathVariable UUID tripId) {
        return geoRepository.findByTripId(tripId);
    }

    /** Called by the device's background push handler after a silent-push GPS fix. */
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
        return geoRepository.save(location);
    }
}
