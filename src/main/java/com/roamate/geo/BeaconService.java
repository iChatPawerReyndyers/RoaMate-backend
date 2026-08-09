package com.roamate.geo;

import com.roamate.geo.dto.RaiseBeaconRequest;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BeaconService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final BeaconAlertRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public BeaconService(BeaconAlertRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * GEO-05: shared by the live HTTP endpoint (BeaconController) and the
     * offline-sync replay path (BeaconAlertRaisedApplier) - a beacon raised
     * while offline and synced later still needs to broadcast, not just
     * silently persist.
     */
    @Transactional
    public BeaconAlert raise(RaiseBeaconRequest request) {
        BeaconAlert alert = new BeaconAlert();
        alert.setTripId(request.tripId());
        alert.setRaisedByUserId(request.raisedByUserId());
        alert.setCoordinates(GEOMETRY_FACTORY.createPoint(new Coordinate(request.lng(), request.lat())));
        alert.setStatus(request.status() != null ? request.status() : BeaconStatus.NEED_ASSISTANCE);
        alert.setMessage(request.message());
        alert.setDestinationId(request.destinationId());
        alert.setRaisedAt(request.raisedAt() != null ? request.raisedAt() : Instant.now());

        BeaconAlert saved = repository.save(alert);
        messagingTemplate.convertAndSend("/topic/trips/" + saved.getTripId() + "/beacons", saved);
        return saved;
    }
}
