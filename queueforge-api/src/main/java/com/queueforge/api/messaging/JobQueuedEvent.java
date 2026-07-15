package com.queueforge.api.messaging;

import com.queueforge.common.messaging.JobMessage;

/**
 * Raised when a job has been persisted in QUEUED state. The message is published
 * to RabbitMQ only after the surrounding transaction commits, so consumers never
 * see the job before its state is durable.
 */
public record JobQueuedEvent(JobMessage message) {
}
