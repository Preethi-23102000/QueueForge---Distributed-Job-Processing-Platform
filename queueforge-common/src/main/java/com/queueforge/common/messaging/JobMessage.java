package com.queueforge.common.messaging;

import com.queueforge.common.enums.JobType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The message published to RabbitMQ by the API and consumed by the Worker.
 * Keep it small: carry the job id + just enough to process; large data
 * stays in PostgreSQL.
 */
public record JobMessage(
    UUID jobId,
    JobType jobType,
    int attempt,
    Map<String, Object> payload,
    Instant createdAt
) implements Serializable {
}