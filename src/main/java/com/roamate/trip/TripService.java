package com.roamate.trip;

import com.roamate.trip.dto.CreateTripRequest;
import com.roamate.trip.dto.JoinTripRequest;
import org.hibernate.Hibernate;
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
        trip.setInviteSecret(generateInviteSecret());
        tripRepository.save(trip);

        TripMember owner = new TripMember();
        owner.setTrip(trip);
        owner.setUserId(ownerUserId);
        owner.setDisplayName(ownerDisplayName);
        owner.setRole(TripRole.OWNER);
        tripMemberRepository.save(owner);

        // trip.getMembers() is a lazy collection; touching it later (e.g. in
        // TripController.toDto) happens after this @Transactional method has
        // returned and its session is closed (open-in-view is off), which
        // throws LazyInitializationException. Force it to load now, while
        // the session is still open, so it's just plain in-memory data by
        // the time the controller reads it.
        Hibernate.initialize(trip.getMembers());
        return trip;
    }

    @Transactional
    public TripMember joinTrip(JoinTripRequest request, String userId) {
        Trip trip = tripRepository.findByInviteCode(request.inviteCode().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        // TRIP-01: a request that arrived via ScanQRScreen carries the QR's
        // embedded inviteSecret alongside the code. Typed-code joins (no
        // secret in the request) still work exactly as before - this only
        // adds an extra check on the QR path, it never blocks manual entry.
        if (request.inviteSecret() != null && !request.inviteSecret().isBlank()
                && !constantTimeEquals(request.inviteSecret(), trip.getInviteSecret())) {
            throw new IllegalArgumentException("Invalid invite code");
        }

        TripMember member = tripMemberRepository.findByTripIdAndUserId(trip.getId(), userId)
                .orElseGet(() -> {
                    TripMember newMember = new TripMember();
                    newMember.setTrip(trip);
                    newMember.setUserId(userId);
                    newMember.setDisplayName(request.displayName());
                    newMember.setRole(TripRole.MEMBER);
                    return tripMemberRepository.save(newMember);
                });

        // Same reasoning as createTrip: trip was loaded fresh from the DB,
        // so members is a genuine unfetched lazy proxy here - initialize it
        // before the transaction (and its session) closes.
        Hibernate.initialize(trip.getMembers());
        return member;
    }

    public List<TripMember> listMembers(UUID tripId) {
        return tripMemberRepository.findByTripId(tripId);
    }

    /** GEO-01: persist the "Share My Location" toggle so GEO-03's silent-push fan-out knows who opted in. */
    @Transactional
    public TripMember setLocationSharing(UUID tripId, String userId, boolean enabled) {
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this trip"));
        member.setLocationSharingEnabled(enabled);
        return tripMemberRepository.save(member);
    }

    /** Every trip this user owns or has joined, most recently created first. */
    @Transactional(readOnly = true)
    public List<Trip> listTripsForUser(String userId) {
        List<Trip> trips = tripMemberRepository.findByUserId(userId).stream()
                .map(TripMember::getTrip)
                .distinct()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        trips.forEach(trip -> Hibernate.initialize(trip.getMembers()));
        return trips;
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

    /**
     * 256 bits of randomness as hex - the QR-only secret from TRIP-01.
     * Doesn't need a uniqueness check like inviteCode does: it's never
     * looked up by itself, only compared against a trip already found by
     * inviteCode, so a collision would be practically impossible and
     * harmless even if it happened.
     */
    private String generateInviteSecret() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(
                a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}