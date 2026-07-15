package com.queueforge.worker.listener;

import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.messaging.QueueForgeQueues;
import com.queueforge.worker.processor.JobProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Receives job messages from RabbitMQ and hands them to the processor.
 */
@Component
public class JobListener {

    private final JobProcessor jobProcessor;

    public JobListener(JobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;
    }

    @RabbitListener(queues = QueueForgeQueues.JOBS_QUEUE)
    public void onJobMessage(JobMessage message) {
        jobProcessor.process(message);
    }
}
