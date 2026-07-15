package com.queueforge.api.service;

import com.queueforge.common.dto.JobExecutionResponse;
import com.queueforge.common.dto.JobResponse;
import com.queueforge.common.entity.Job;
import com.queueforge.common.entity.JobExecution;

/**
 * Maps persistence entities to API response DTOs.
 */
public final class JobMapper {

    private JobMapper() {
    }

    public static JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getJobType(),
                job.getStatus(),
                job.getPayload(),
                job.getRetryCount(),
                job.getMaxRetries(),
                job.getResult(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getStartedAt(),
                job.getCompletedAt());
    }

    public static JobExecutionResponse toResponse(JobExecution execution) {
        return new JobExecutionResponse(
                execution.getId(),
                execution.getJobId(),
                execution.getAttemptNumber(),
                execution.getStatus(),
                execution.getWorkerId(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getDurationMs(),
                execution.getErrorMessage());
    }
}
