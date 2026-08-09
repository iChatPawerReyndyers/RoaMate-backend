package com.roamate.trip;

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

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public TripDto createTrip(@Valid @RequestBody CreateTripRequest request,
                              @AuthenticationPrincipal String userId) {
        Trip trip = tripService.createTrip(request, userId, request.name());
        return toDto(trip);
    }

    @PostMapping("/join")
    public TripDto joinTrip(@Valid @RequestBody JoinTripRequest request,
                            @AuthenticationPrincipal String userId) {
        TripMember member = tripService.joinTrip(request, userId);
        return toDto(member.getTrip());
    }

    @GetMapping("/{tripId}/members")
    public List<TripMemberDto> listMembers(@PathVariable UUID tripId) {
        return tripService.listMembers(tripId).stream().map(this::toDto).collect(Collectors.toList());
    }

    private TripDto toDto(Trip trip) {
        List<TripMemberDto> members = trip.getMembers().stream()
                .map(member -> new TripMemberDto(member.getId(), member.getTrip().getId(), member.getUserId(), member.getDisplayName(), member.getRole().name()))
                .collect(Collectors.toList());

        return new TripDto(trip.getId(), trip.getName(), trip.getStartDate(), trip.getEndDate(), trip.getInviteCode(), trip.getDefaultCurrency(), members);
    }

    private TripMemberDto toDto(TripMember member) {
        return new TripMemberDto(member.getId(), member.getTrip().getId(), member.getUserId(), member.getDisplayName(), member.getRole().name());
    }
}
