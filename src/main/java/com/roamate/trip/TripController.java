package com.roamate.trip;

import com.roamate.auth.User;
import com.roamate.auth.UserRepository;
import com.roamate.trip.dto.CreateTripRequest;
import com.roamate.trip.dto.JoinTripRequest;
import com.roamate.trip.dto.TripDto;
import com.roamate.trip.dto.TripMemberDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;
    private final UserRepository userRepository;

    public TripController(TripService tripService, UserRepository userRepository) {
        this.tripService = tripService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<TripDto> listMyTrips(@AuthenticationPrincipal(expression = "subject") String userId) {
        return tripService.listTripsForUser(userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @PostMapping
    public TripDto createTrip(@Valid @RequestBody CreateTripRequest request,
                              @AuthenticationPrincipal(expression = "subject") String userId) {
        Trip trip = tripService.createTrip(request, userId, ownerDisplayName(userId));
        return toDto(trip);
    }

    @PostMapping("/join")
    public TripDto joinTrip(@Valid @RequestBody JoinTripRequest request,
                            @AuthenticationPrincipal(expression = "subject") String userId) {
        TripMember member = tripService.joinTrip(request, userId);
        return toDto(member.getTrip());
    }

    @GetMapping("/{tripId}/members")
    public List<TripMemberDto> listMembers(@PathVariable UUID tripId) {
        return tripService.listMembers(tripId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public record LocationSharingRequest(boolean enabled) {}

    /** GEO-01: "Share My Location" (ON/OFF), defaults to OFF. */
    @PutMapping("/{tripId}/members/me/location-sharing")
    public TripMemberDto setLocationSharing(@PathVariable UUID tripId,
                                            @RequestBody LocationSharingRequest request,
                                            @AuthenticationPrincipal(expression = "subject") String userId) {
        return toDto(tripService.setLocationSharing(tripId, userId, request.enabled()));
    }

    /**
     * Was previously just request.name() at the createTrip call site - the
     * trip's own name, not the owner's identity, so every trip's owner
     * displayed as e.g. "Bali Trip" instead of themselves. Now resolves the
     * account's username. userId being a non-UUID (the legacy dev-login
     * flow's raw device id, kept for backward compatibility) falls back to
     * showing the id itself rather than failing the request.
     */
    private String ownerDisplayName(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .map(User::getUsername)
                    .orElse(userId);
        } catch (IllegalArgumentException notARealAccountId) {
            return userId;
        }
    }

    private TripDto toDto(Trip trip) {
        List<TripMemberDto> members = trip.getMembers().stream()
                .map(member -> new TripMemberDto(member.getId(), member.getTrip().getId(), member.getUserId(), member.getDisplayName(), member.getRole().name(), member.isLocationSharingEnabled()))
                .collect(Collectors.toList());

        return new TripDto(trip.getId(), trip.getName(), trip.getDescription(), trip.getStartDate(), trip.getEndDate(), trip.getInviteCode(), trip.getInviteSecret(), trip.getDefaultCurrency(), members);
    }

    private TripMemberDto toDto(TripMember member) {
        return new TripMemberDto(member.getId(), member.getTrip().getId(), member.getUserId(), member.getDisplayName(), member.getRole().name(), member.isLocationSharingEnabled());
    }
}