package com.queueforge.api.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes queued jobs to RabbitMQ only after the transaction that created them
 * has committed, avoiding a race where the worker reads the job before its new
 * state is visible.
 */
@Component
public class JobQueuedEventListener {

    private final JobPublisher jobPublisher;

    public JobQueuedEventListener(JobPublisher jobPublisher) {
        this.jobPublisher = jobPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobQueued(JobQueuedEvent event) {
        jobPublisher.publish(event.message());
    }
}
