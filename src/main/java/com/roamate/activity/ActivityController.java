package com.roamate.activity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/sessions")
    public ActivitySession record(@RequestBody ActivitySession session) {
        return activityService.recordBatch(session);
    }

    @GetMapping("/trips/{tripId}/users/{userId}/sessions")
    public List<ActivitySession> history(@PathVariable UUID tripId, @PathVariable String userId) {
        return activityService.history(tripId, userId);
    }
}
