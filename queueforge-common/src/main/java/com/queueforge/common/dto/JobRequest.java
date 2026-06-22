package com.queueforge.common.dto;

import com.queueforge.common.enums.JobType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Incoming payload for POST /api/v1/jobs.
 */
public record JobRequest(

    @NotNull(message = "jobType is required")
    JobType jobType,

    Map<String, Object> payload,

    @Min(value = 0, message = "maxRetries must be >= 0")
    Integer maxRetries
) {
}