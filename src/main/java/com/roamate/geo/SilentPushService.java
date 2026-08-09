package com.roamate.geo;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * GEO-02/03: RoaMate never polls location in the background. Instead, when
 * a member opens the map, the server sends a SILENT push (content-available,
 * no user-visible notification) to every other member's device, which wakes
 * the app briefly to capture one GPS fix and POST it back. If a device
 * doesn't respond within the hard 5-second timeout (GEO-04), the map falls
 * back to that member's last-cached position with a visible "Updated X
 * mins ago" age indicator rather than blocking the requester.
 */
@Service
public class SilentPushService {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Sends the silent push and returns a future that completes either with
     * the fresh location payload, or times out - callers should catch the
     * timeout and fall back to GeoRepository's last-cached row.
     */
    public CompletableFuture<LocationPingResult> requestLocation(String fcmOrApnsToken, String userId) {
        CompletableFuture<LocationPingResult> future = new CompletableFuture<>();

        // In production this dispatches a real FCM/APNs silent (background) push
        // via firebase-admin / Apple's HTTP/2 provider API and resolves `future`
        // when the device's push-triggered background handler POSTs its fix to
        // POST /api/v1/geo/locations. Wiring omitted here (needs live credentials).
        dispatchSilentPush(fcmOrApnsToken, userId);

        return future.completeOnTimeout(LocationPingResult.timedOut(userId), RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void dispatchSilentPush(String token, String userId) {
        // See config/ for FCM/APNs client wiring.
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
