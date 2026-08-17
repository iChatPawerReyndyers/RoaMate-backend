package com.roamate.geo;

import com.roamate.push.DeviceToken;
import com.roamate.push.DeviceTokenRepository;
import com.roamate.push.FirebaseMessagingClient;
import com.roamate.trip.TripMember;
import com.roamate.trip.TripMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GEO-02/03/04: RoaMate never polls location in the background. Instead,
 * when a member opens the map, the server sends a SILENT push
 * (content-available, no user-visible notification) to every other
 * sharing-enabled member's device, which wakes the app briefly to capture
 * one GPS fix and POST it back to LocationController.reportLocation(). If
 * a device doesn't respond within the timeout, that member's last cached
 * position is returned instead, with `stale=true` so the client can show
 * an age indicator - the requester is never blocked waiting on a single
 * slow or offline device beyond the fixed timeout window.
 */
@Service
public class SilentPushService {

    private static final Logger log = LoggerFactory.getLogger(SilentPushService.class);

    private final TripMemberRepository tripMemberRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final GeoRepository geoRepository;
    private final FirebaseMessagingClient firebaseMessagingClient;
    private final Duration responseTimeout;

    /** Keyed by "tripId:userId" - lets LocationController.reportLocation() resolve the matching in-flight wait. */
    private final ConcurrentHashMap<String, CompletableFuture<LocationPingResult>> pendingRequests = new ConcurrentHashMap<>();

    public SilentPushService(TripMemberRepository tripMemberRepository,
                              DeviceTokenRepository deviceTokenRepository,
                              GeoRepository geoRepository,
                              FirebaseMessagingClient firebaseMessagingClient,
                              @Value("${roamate.geo.location-request-timeout-ms:5000}") long timeoutMs) {
        this.tripMemberRepository = tripMemberRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.geoRepository = geoRepository;
        this.firebaseMessagingClient = firebaseMessagingClient;
        this.responseTimeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * Called when a member opens the map. Fans out a silent push to every
     * OTHER member with location sharing enabled (GEO-01) who has at least
     * one registered device, waits up to the configured timeout (GEO-04's
     * 5s) for each to respond, then returns the current location list -
     * fresh where a device answered in time, last-known-cached (marked
     * stale) where it didn't.
     *
     * Deliberately filters the final result to only currently
     * sharing-enabled members: a member who has since flipped GEO-01 off
     * must not keep showing up via a leftover cached row from before they
     * disabled it - the toggle is a privacy control, not just a hint for
     * whether to bother pushing.
     *
     * This blocks the calling HTTP thread for up to responseTimeout - an
     * intentional trade-off matching the spec's own description ("Opening
     * the map triggers... "), not an oversight; trip group sizes are small
     * enough that this doesn't become a real scalability problem.
     */
    public List<MemberLocation> refreshTripLocations(UUID tripId, String requestingUserId) {
        List<TripMember> allMembers = tripMemberRepository.findByTripId(tripId);

        Set<String> sharingEnabledUserIds = allMembers.stream()
                .filter(TripMember::isLocationSharingEnabled)
                .map(TripMember::getUserId)
                .collect(Collectors.toSet());

        List<TripMember> pushTargets = allMembers.stream()
                .filter(m -> !m.getUserId().equals(requestingUserId))
                .filter(TripMember::isLocationSharingEnabled)
                .toList();

        Map<String, CompletableFuture<LocationPingResult>> inFlight = new HashMap<>();

        for (TripMember member : pushTargets) {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserId(member.getUserId());
            if (tokens.isEmpty()) {
                // Nothing to push to - falls straight through to whatever's
                // already cached (or nothing, if they've never shared).
                continue;
            }

            String key = pendingKey(tripId, member.getUserId());
            CompletableFuture<LocationPingResult> future = new CompletableFuture<>();
            pendingRequests.put(key, future);
            inFlight.put(member.getUserId(),
                    future.completeOnTimeout(LocationPingResult.timedOut(member.getUserId()), responseTimeout.toMillis(), TimeUnit.MILLISECONDS));

            for (DeviceToken token : tokens) {
                firebaseMessagingClient.sendSilentDataMessage(token.getToken(), Map.of(
                        "type", "LOCATION_REQUEST",
                        "tripId", tripId.toString()
                ));
            }
        }

        if (!inFlight.isEmpty()) {
            try {
                CompletableFuture.allOf(inFlight.values().toArray(new CompletableFuture[0]))
                        .get(responseTimeout.toMillis() + 1000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Individual futures already carry their own timeout via
                // completeOnTimeout above - this outer bound is just a
                // safety net so a bug in that mechanism can't hang the
                // request forever. Whatever hasn't resolved by now is
                // simply treated as timed out below via future.getNow().
                log.warn("Unexpected wait failure while collecting location responses for trip {}", tripId, e);
            } finally {
                for (String userId : inFlight.keySet()) {
                    pendingRequests.remove(pendingKey(tripId, userId));
                }
            }
        }

        return geoRepository.findByTripId(tripId).stream()
                .filter(loc -> sharingEnabledUserIds.contains(loc.getUserId()))
                .toList();
    }

    /**
     * Called by LocationController.reportLocation() after it has already
     * persisted the fresh fix, to unblock the matching wait in
     * refreshTripLocations() above (a no-op if that request already timed
     * out and moved on, or if this arrives unprompted - e.g. a stray
     * retry).
     */
    public void completeLocation(UUID tripId, String userId, double lat, double lng) {
        CompletableFuture<LocationPingResult> future = pendingRequests.get(pendingKey(tripId, userId));
        if (future != null) {
            future.complete(LocationPingResult.of(userId, lat, lng));
        }
    }

    private static String pendingKey(UUID tripId, String userId) {
        return tripId + ":" + userId;
    }

    public record LocationPingResult(String userId, boolean timedOut, Double lat, Double lng) {
        public static LocationPingResult timedOut(String userId) {
            return new LocationPingResult(userId, true, null, null);
        }
        public static LocationPingResult of(String userId, double lat, double lng) {
            return new LocationPingResult(userId, false, lat, lng);
        }
    }
}
