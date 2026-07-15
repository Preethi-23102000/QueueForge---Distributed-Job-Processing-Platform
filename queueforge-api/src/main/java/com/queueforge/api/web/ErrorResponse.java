package com.queueforge.api.web;

import java.time.Instant;

/**
 * Standard error body returned by the API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message) {
}
