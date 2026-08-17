package com.roamate.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Without this, every bare IllegalArgumentException/IllegalStateException
 * thrown from a service (e.g. TripService.joinTrip's "Invalid invite code"
 * for a typo'd code) fell through Spring's default handling as a raw 500
 * Internal Server Error - indistinguishable from an actual server bug,
 * both to the client and to whoever's reading the logs. Found via tracing
 * the join-trip flow end to end; grepping turned up 8 other call sites
 * with the identical gap (checklist conversion, settlement validation,
 * duplicate detection, invite/membership lookups), so this is a systemic
 * fix rather than a one-off patch for join-trip specifically.
 *
 * IllegalArgumentException -> 400: the request itself was invalid (bad
 * invite code, bad payment split, expense that doesn't exist).
 * IllegalStateException -> 409: the request was well-formed but conflicts
 * with the current state of something (checklist item already converted).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    public record ErrorResponse(String message) {}
}