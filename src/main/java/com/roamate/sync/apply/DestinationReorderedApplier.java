package com.roamate.sync.apply;

import tools.jackson.databind.json.JsonMapper;
import com.roamate.itinerary.ItineraryService;
import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DestinationReorderedApplier implements EventApplier {

    private final ItineraryService itineraryService;
    private final JsonMapper jsonMapper;

    public DestinationReorderedApplier(ItineraryService itineraryService, JsonMapper jsonMapper) {
        this.itineraryService = itineraryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EventType supportedType() {
        return EventType.DESTINATION_REORDERED;
    }

    @Override
    public void apply(EventLogEntity event) throws Exception {
        Payload payload = jsonMapper.readValue(event.getPayloadJson(), Payload.class);
        itineraryService.reorder(payload.reorder);
    }

    private record Payload(List<UUID> reorder) {}
}
