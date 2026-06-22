package com.queueforge.common.dto;

import com.queueforge.common.enums.JobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * One processing attempt of a job (execution history).
 */
public record JobExecutionResponse(
    UUID id,
    UUID jobId,
    int attemptNumber,
    JobStatus status,
    String workerId,
    Instant startedAt,
    Instant completedAt,
    Long durationMs,
    String errorMessage
) {
}