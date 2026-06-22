package com.queueforge.common.dto;

import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * What the API returns when a client queries a job.
 */
public record JobResponse(
    UUID jobId,
    JobType jobType,
    JobStatus status,
    Map<String, Object> payload,
    Integer retryCount,
    Integer maxRetries,
    Map<String, Object> result,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt,
    Instant startedAt,
    Instant completedAt
) {
}