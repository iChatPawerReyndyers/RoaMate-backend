package com.roamate.sync.apply;

import tools.jackson.databind.json.JsonMapper;
import com.roamate.geo.BeaconService;
import com.roamate.geo.dto.RaiseBeaconRequest;
import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;
import org.springframework.stereotype.Component;

@Component
public class BeaconAlertRaisedApplier implements EventApplier {

    private final BeaconService beaconService;
    private final JsonMapper jsonMapper;

    public BeaconAlertRaisedApplier(BeaconService beaconService, JsonMapper jsonMapper) {
        this.beaconService = beaconService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EventType supportedType() {
        return EventType.BEACON_ALERT_RAISED;
    }

    @Override
    public void apply(EventLogEntity event) throws Exception {
        // The queued payload is already shaped exactly like RaiseBeaconRequest
        // (see EmergencyBeacon.tsx's offline-queue fallback), so it deserializes directly.
        RaiseBeaconRequest request = jsonMapper.readValue(event.getPayloadJson(), RaiseBeaconRequest.class);
        beaconService.raise(request);
    }
}
