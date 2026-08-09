package com.roamate.trip;

import com.roamate.trip.dto.CreateTripRequest;
import com.roamate.trip.dto.JoinTripRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I ambiguity
    private static final SecureRandom RNG = new SecureRandom();

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;

    public TripService(TripRepository tripRepository, TripMemberRepository tripMemberRepository) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
    }

    @Transactional
    public Trip createTrip(CreateTripRequest request, String ownerUserId, String ownerDisplayName) {
        Trip trip = new Trip();
        trip.setName(request.name());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        if (request.defaultCurrency() != null) {
            trip.setDefaultCurrency(request.defaultCurrency());
        }
        trip.setInviteCode(generateUniqueInviteCode());
        tripRepository.save(trip);

        TripMember owner = new TripMember();
        owner.setTrip(trip);
        owner.setUserId(ownerUserId);
        owner.setDisplayName(ownerDisplayName);
        owner.setRole(TripRole.OWNER);
        tripMemberRepository.save(owner);

        return trip;
    }

    @Transactional
    public TripMember joinTrip(JoinTripRequest request, String userId) {
        Trip trip = tripRepository.findByInviteCode(request.inviteCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        return tripMemberRepository.findByTripIdAndUserId(trip.getId(), userId)
                .orElseGet(() -> {
                    TripMember member = new TripMember();
                    member.setTrip(trip);
                    member.setUserId(userId);
                    member.setDisplayName(request.displayName());
                    member.setRole(TripRole.MEMBER);
                    return tripMemberRepository.save(member);
                });
    }

    public List<TripMember> listMembers(UUID tripId) {
        return tripMemberRepository.findByTripId(tripId);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RNG.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (tripRepository.findByInviteCode(code).isPresent());
        return code;
    }
}
