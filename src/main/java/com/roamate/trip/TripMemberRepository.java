package com.roamate.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {
    List<TripMember> findByTripId(UUID tripId);
    Optional<TripMember> findByTripIdAndUserId(UUID tripId, String userId);
    List<TripMember> findByUserId(String userId);
}