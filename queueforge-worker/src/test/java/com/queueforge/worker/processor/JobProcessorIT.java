package com.queueforge.worker.processor;

import com.queueforge.common.entity.Job;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.messaging.QueueForgeQueues;
import com.queueforge.common.repository.JobExecutionRepository;
import com.queueforge.common.repository.JobRepository;
import com.queueforge.worker.AbstractWorkerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the full asynchronous pipeline: a message is published to RabbitMQ,
 * the worker consumes it, processes the job, and updates Postgres. Covers the
 * success path and the failure -> retry -> dead-letter path.
 */
class JobProcessorIT extends AbstractWorkerIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository executionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void processesJobThroughToCompleted() {
        Map<String, Object> payload = Map.of("sleepMs", 100);
        UUID id = jobRepository.save(newQueuedJob(payload, 3)).getId();

        publish(id, payload, 1);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(jobRepository.findById(id)).get()
                        .extracting(Job::getStatus).isEqualTo(JobStatus.COMPLETED));

        assertThat(executionRepository.findByJobIdOrderByAttemptNumberAsc(id)).hasSize(1);
    }

    @Test
    void deadLettersJobAfterExhaustingRetries() {
        Map<String, Object> payload = Map.of("simulateFailure", true, "sleepMs", 100);
        UUID id = jobRepository.save(newQueuedJob(payload, 1)).getId();

        publish(id, payload, 1);

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() ->
                assertThat(jobRepository.findById(id)).get()
                        .extracting(Job::getStatus).isEqualTo(JobStatus.DEAD_LETTERED));

        // one initial attempt + one retry, both failed
        assertThat(executionRepository.findByJobIdOrderByAttemptNumberAsc(id)).hasSize(2);
    }

    private void publish(UUID jobId, Map<String, Object> payload, int attempt) {
        rabbitTemplate.convertAndSend(
                QueueForgeQueues.EXCHANGE,
                QueueForgeQueues.JOB_ROUTING_KEY,
                new JobMessage(jobId, JobType.REPORT_GENERATION, attempt, payload, Instant.now()));
    }

    private Job newQueuedJob(Map<String, Object> payload, int maxRetries) {
        Job job = new Job();
        job.setJobType(JobType.REPORT_GENERATION);
        job.setStatus(JobStatus.QUEUED);
        job.setPayload(payload);
        job.setRetryCount(0);
        job.setMaxRetries(maxRetries);
        return job;
    }
}
