package com.roamate.geo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeoRepository extends JpaRepository<MemberLocation, UUID> {
    List<MemberLocation> findByTripId(UUID tripId);
    Optional<MemberLocation> findByTripIdAndUserId(UUID tripId, String userId);

    /** Members within `radiusMeters` of a point - uses the PostGIS GIST index on `coordinates`. */
    @Query(value = "SELECT * FROM member_locations m WHERE m.trip_id = :tripId " +
            "AND ST_DWithin(m.coordinates, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)",
            nativeQuery = true)
    List<MemberLocation> findWithinRadius(UUID tripId, double lat, double lng, double radiusMeters);
}
