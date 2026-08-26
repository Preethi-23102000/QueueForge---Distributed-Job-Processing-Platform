package com.queueforge.worker.messaging;

import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.messaging.QueueForgeQueues;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Republishes jobs for another attempt (with a backoff delay) or routes them to
 * the dead-letter queue once their retries are exhausted.
 */
@Component
public class JobRetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobRetryPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends the job to the retry queue with a per-message expiration. When the
     * message expires it dead-letters back to the main exchange, so the job is
     * re-delivered to a worker after roughly {@code delayMs}.
     */
    public void republishWithDelay(JobMessage message, long delayMs) {
        rabbitTemplate.convertAndSend(
                QueueForgeQueues.RETRY_EXCHANGE,
                QueueForgeQueues.RETRY_ROUTING_KEY,
                message,
                m -> {
                    m.getMessageProperties().setExpiration(Long.toString(delayMs));
                    return m;
                });
    }

    /**
     * Sends the job to the dead-letter queue for manual inspection.
     */
    public void sendToDeadLetter(JobMessage message) {
        rabbitTemplate.convertAndSend(
                QueueForgeQueues.DLX_EXCHANGE,
                QueueForgeQueues.DLQ_ROUTING_KEY,
                message);
    }
}
