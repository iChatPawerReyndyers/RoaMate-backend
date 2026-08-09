package com.roamate.activity;

import com.roamate.activity.dto.DestinationActivitySummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivitySessionRepository repository;

    public ActivityService(ActivitySessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ActivitySession recordBatch(ActivitySession session) {
        return repository.save(session);
    }

    public List<ActivitySession> history(UUID tripId, String userId) {
        return repository.findByTripIdAndUserId(tripId, userId);
    }

    /**
     * ACT-04: rolls up every session attached to this destination (any
     * member, any activity type) into the single metrics row the Pinned
     * Location Card shows. Distance/elevation/steps sum across sessions;
     * cave depth takes the max reading rather than summing, since depth is
     * a single-session measurement, not a cumulative one.
     */
    public DestinationActivitySummary destinationSummary(UUID destinationId) {
        List<ActivitySession> sessions = repository.findByDestinationId(destinationId);

        double distance = sessions.stream().mapToDouble(s -> nz(s.getDistanceMeters())).sum();
        double elevationGain = sessions.stream().mapToDouble(s -> nz(s.getElevationGainMeters())).sum();
        double maxDepth = sessions.stream().mapToDouble(s -> nz(s.getRelativeDepthMeters())).max().orElse(0);
        int steps = sessions.stream().mapToInt(s -> s.getStepCount() == null ? 0 : s.getStepCount()).sum();

        return new DestinationActivitySummary(destinationId, distance, elevationGain, maxDepth, steps, sessions.size());
    }

    private static double nz(Double value) {
        return value == null ? 0 : value;
    }
}
