package com.roamate.activity.dto;

import java.util.UUID;

/**
 * ACT-04 / section 7.2 mockup: aggregated metrics for everything logged
 * against a single destination, across all trip members - e.g. the
 * "6.4 km / 850 m / 9,420 steps" row on a Pinned Location Card.
 */
public record DestinationActivitySummary(
        UUID destinationId,
        double totalDistanceMeters,
        double totalElevationGainMeters,
        double maxRelativeDepthMeters,
        int totalSteps,
        int sessionCount
) {}
