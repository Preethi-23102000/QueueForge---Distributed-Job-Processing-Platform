package com.queueforge.common.messaging;

/**
 * Shared RabbitMQ topology names so the API (publisher) and Worker (consumer)
 * always agree. Retry + dead-letter queues are added in Stage 2.
 */
public final class QueueForgeQueues {

  private QueueForgeQueues() {
  }

  public static final String EXCHANGE = "queueforge.exchange";
  public static final String JOBS_QUEUE = "queueforge.jobs";
  public static final String JOB_ROUTING_KEY = "job.execute";
}