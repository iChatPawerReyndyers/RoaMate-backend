package com.roamate.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Thin wrapper around the Firebase Admin SDK for GEO-02/03's silent push.
 * Deliberately never throws/crashes app startup if no credentials are
 * configured (roamate.push.fcm-credentials-path unset, or the file it
 * points to doesn't exist) - a dev environment without a real Firebase
 * project should still boot and run everything else; push just becomes a
 * no-op with a clear warning logged once. Same philosophy as this app's
 * other optional external config (see AuthController's dev-only JWT
 * secret default).
 *
 * Only FCM is wired here, not a separate raw APNs client, even though
 * application.yml also has a roamate.push.apns-key-path slot: the mobile
 * client uses @react-native-firebase/messaging on both platforms, which
 * registers an FCM token on iOS too (FCM relays to APNs under the hood
 * given a Firebase project with APNs credentials attached) - so one FCM
 * send addresses either platform, and a second direct-APNs code path would
 * be pure duplication for no behavioral difference.
 */
@Component
public class FirebaseMessagingClient {

    private static final Logger log = LoggerFactory.getLogger(FirebaseMessagingClient.class);

    private final boolean configured;

    public FirebaseMessagingClient(@Value("${roamate.push.fcm-credentials-path:}") String credentialsPath) {
        this.configured = initialize(credentialsPath);
    }

    private boolean initialize(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("roamate.push.fcm-credentials-path is not set - silent push (GEO-02/03) is disabled; "
                    + "the map will fall back to each member's last-known cached location (GEO-04) instead.");
            return false;
        }
        Path path = Path.of(credentialsPath);
        if (!Files.exists(path)) {
            log.warn("FCM credentials file not found at {} - silent push (GEO-02/03) is disabled.", credentialsPath);
            return false;
        }
        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            return true;
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase Admin SDK - silent push (GEO-02/03) is disabled.", e);
            return false;
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    /**
     * Sends a silent (content-available, data-only) push - no visible
     * notification, no sound, no banner. Fire-and-forget: the actual
     * "did it work" signal is whether the device POSTs a location back
     * within the timeout window (see SilentPushService), not the send
     * call's own success/failure.
     */
    public void sendSilentDataMessage(String token, Map<String, String> data) {
        if (!configured) {
            log.debug("sendSilentDataMessage called but Firebase isn't configured - skipping (token={})", token);
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setContentAvailable(true).build())
                            // Background/silent pushes must use APNs priority 5, not the
                            // default 10 (immediate delivery) - 10 is rejected by APNs
                            // for content-available-only payloads with no alert/sound.
                            .putHeader("apns-priority", "5")
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("Failed to send silent push to token={}", token, e);
        }
    }
}
