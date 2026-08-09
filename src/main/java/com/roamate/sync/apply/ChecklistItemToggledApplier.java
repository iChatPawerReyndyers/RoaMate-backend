package com.roamate.sync.apply;

import tools.jackson.databind.json.JsonMapper;
import com.roamate.checklist.ChecklistService;
import com.roamate.sync.EventLogEntity;
import com.roamate.sync.EventType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChecklistItemToggledApplier implements EventApplier {

    private final ChecklistService checklistService;
    private final JsonMapper jsonMapper;

    public ChecklistItemToggledApplier(ChecklistService checklistService, JsonMapper jsonMapper) {
        this.checklistService = checklistService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public EventType supportedType() {
        return EventType.CHECKLIST_ITEM_TOGGLED;
    }

    @Override
    public void apply(EventLogEntity event) throws Exception {
        Payload payload = jsonMapper.readValue(event.getPayloadJson(), Payload.class);
        checklistService.toggle(payload.itemId);
    }

    /**
     * Note: this replays as an unconditional toggle, same as the live
     * direct-call path already does - it doesn't try to detect whether the
     * item was independently toggled by someone else in the meantime.
     * That's a real limitation (see the pinned architecture note on
     * conflict handling), not something this applier alone can fix.
     */
    private record Payload(UUID itemId, String toggledAt) {}
}
