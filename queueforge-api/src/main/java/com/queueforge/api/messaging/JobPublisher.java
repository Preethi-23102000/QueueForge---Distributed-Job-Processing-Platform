package com.queueforge.api.messaging;

import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.messaging.QueueForgeQueues;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes job messages to the QueueForge exchange for workers to consume.
 */
@Component
public class JobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public JobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(JobMessage message) {
        rabbitTemplate.convertAndSend(
                QueueForgeQueues.EXCHANGE,
                QueueForgeQueues.JOB_ROUTING_KEY,
                message);
    }
}
