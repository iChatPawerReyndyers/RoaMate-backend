package com.roamate.sync.apply;

import tools.jackson.databind.json.JsonMapper;
import com.roamate.itinerary.ItineraryService;
import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACT-05: replays a "Finish activity at this stop" tap that happened while
 * offline (see ItineraryController.markActivityCompleted for the
 * online/direct-call path, and MapScreen/ActivityDashboardScreen's
 * companion frontend change for where this gets queued). Mirrors
 * DestinationReorderedApplier's shape exactly.
 */
@Component
public class DestinationActivityCompletedApplier implements EventApplier {

    private final ItineraryService itineraryService;
    private final JsonMapper jsonMapper;

    public DestinationActivityCompletedApplier(ItineraryService itineraryService, JsonMapper jsonMapper) {
        this.itineraryService = itineraryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EventType supportedType() {
        return EventType.DESTINATION_ACTIVITY_COMPLETED;
    }

    @Override
    public void apply(EventLogEntity event) throws Exception {
        Payload payload = jsonMapper.readValue(event.getPayloadJson(), Payload.class);
        itineraryService.markActivityCompleted(payload.destinationId);
    }

    private record Payload(UUID destinationId) {}
}
