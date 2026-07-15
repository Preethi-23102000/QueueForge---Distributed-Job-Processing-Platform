package com.queueforge.api.exception;

import java.util.UUID;

/**
 * Thrown when a job cannot be found by its identifier.
 */
public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId);
    }
}
