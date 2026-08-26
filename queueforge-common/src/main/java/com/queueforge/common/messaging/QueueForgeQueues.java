package com.queueforge.common.messaging;

/**
 * Shared RabbitMQ topology names so the API (publisher) and Worker (consumer)
 * always agree.
 *
 * <p>Reliability flow: failed-but-retryable jobs are republished to the retry
 * exchange; they wait in the retry queue for a per-message delay, then
 * dead-letter back to the main exchange for another attempt. Jobs that exhaust
 * their retries are published to the dead-letter exchange and land in the DLQ.
 */
public final class QueueForgeQueues {

  private QueueForgeQueues() {
  }

  // Main path: API publishes here, workers consume here.
  public static final String EXCHANGE = "queueforge.exchange";
  public static final String JOBS_QUEUE = "queueforge.jobs";
  public static final String JOB_ROUTING_KEY = "job.execute";

  // Retry path: holds a job for a delay, then dead-letters back to the main queue.
  public static final String RETRY_EXCHANGE = "queueforge.retry.exchange";
  public static final String RETRY_QUEUE = "queueforge.jobs.retry";
  public static final String RETRY_ROUTING_KEY = "job.retry";

  // Dead-letter path: terminal home for jobs that exhausted their retries.
  public static final String DLX_EXCHANGE = "queueforge.dlx";
  public static final String DLQ_QUEUE = "queueforge.jobs.dlq";
  public static final String DLQ_ROUTING_KEY = "job.dead";
}