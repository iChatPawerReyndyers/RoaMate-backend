package com.roamate.geo.dto;

import com.roamate.geo.BeaconStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * GEO-05: flat request shape for raising a beacon. BeaconAlert.coordinates
 * is a JTS Point with no Jackson-visible constructor - same issue
 * LocationController.reportLocation already works around by taking flat
 * lat/lng and building the Point server-side rather than expecting the
 * client to send a Point directly.
 */
public record RaiseBeaconRequest(
        UUID tripId,
        String raisedByUserId,
        double lat,
        double lng,
        BeaconStatus status,
        String message,
        UUID destinationId,
        Instant raisedAt
) {}
