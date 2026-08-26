package com.queueforge.worker.processor;

import com.queueforge.common.entity.Job;
import com.queueforge.common.entity.JobExecution;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.repository.JobExecutionRepository;
import com.queueforge.common.repository.JobRepository;
import com.queueforge.worker.messaging.JobRetryPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Executes a job: transitions it to PROCESSING, simulates work, records an
 * execution attempt, and either completes it, schedules a retry with backoff,
 * or dead-letters it once retries are exhausted.
 */
@Component
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private static final long MIN_SLEEP_MS = 2000;
    private static final long MAX_SLEEP_MS = 5000;
    private static final long SLEEP_CAP_MS = 300000;

    private static final long BASE_BACKOFF_MS = 5000;
    private static final long MAX_BACKOFF_MS = 60000;

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobRetryPublisher retryPublisher;
    private final String workerId;

    public JobProcessor(JobRepository jobRepository,
                        JobExecutionRepository executionRepository,
                        JobRetryPublisher retryPublisher) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.retryPublisher = retryPublisher;
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void process(JobMessage message) {
        Optional<Job> maybeJob = jobRepository.findById(message.jobId());
        if (maybeJob.isEmpty()) {
            log.warn("Received message for unknown job {}", message.jobId());
            return;
        }

        Job job = maybeJob.get();
        if (job.getStatus() != JobStatus.QUEUED && job.getStatus() != JobStatus.RETRYING) {
            log.info("Skipping job {} in status {}", job.getId(), job.getStatus());
            return;
        }

        Instant startedAt = Instant.now();
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(startedAt);
        job = jobRepository.save(job);

        JobExecution execution = new JobExecution();
        execution.setJobId(job.getId());
        execution.setAttemptNumber(message.attempt());
        execution.setStatus(JobStatus.PROCESSING);
        execution.setWorkerId(workerId);
        execution.setStartedAt(startedAt);

        try {
            Map<String, Object> result = runJob(message);

            Instant completedAt = Instant.now();
            long durationMs = Duration.between(startedAt, completedAt).toMillis();

            job.setStatus(JobStatus.COMPLETED);
            job.setResult(result);
            job.setErrorMessage(null);
            job.setCompletedAt(completedAt);
            jobRepository.save(job);

            execution.setStatus(JobStatus.COMPLETED);
            execution.setCompletedAt(completedAt);
            execution.setDurationMs(durationMs);
            executionRepository.save(execution);

            log.info("Job {} completed in {} ms by {}", job.getId(), durationMs, workerId);
        } catch (Exception ex) {
            handleFailure(job, message, execution, startedAt, ex);
        }
    }

    /**
     * Records the failed attempt, then either schedules a delayed retry (if the
     * job has retries left) or moves it to the dead-letter queue.
     */
    private void handleFailure(Job job, JobMessage message, JobExecution execution,
                               Instant startedAt, Exception ex) {
        Instant failedAt = Instant.now();
        long durationMs = Duration.between(startedAt, failedAt).toMillis();

        execution.setStatus(JobStatus.FAILED);
        execution.setCompletedAt(failedAt);
        execution.setDurationMs(durationMs);
        execution.setErrorMessage(ex.getMessage());
        executionRepository.save(execution);

        if (job.getRetryCount() < job.getMaxRetries()) {
            job.setRetryCount(job.getRetryCount() + 1);
            job.setStatus(JobStatus.RETRYING);
            job.setErrorMessage(ex.getMessage());
            Job saved = jobRepository.save(job);

            long delayMs = backoffMs(saved.getRetryCount());
            retryPublisher.republishWithDelay(new JobMessage(
                    saved.getId(), saved.getJobType(), saved.getRetryCount() + 1,
                    saved.getPayload(), Instant.now()), delayMs);

            log.warn("Job {} failed on attempt {} ({}); retry {}/{} scheduled in {} ms",
                    saved.getId(), message.attempt(), ex.getMessage(),
                    saved.getRetryCount(), saved.getMaxRetries(), delayMs);
        } else {
            job.setStatus(JobStatus.DEAD_LETTERED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(failedAt);
            Job saved = jobRepository.save(job);

            retryPublisher.sendToDeadLetter(new JobMessage(
                    saved.getId(), saved.getJobType(), message.attempt(),
                    saved.getPayload(), Instant.now()));

            log.error("Job {} dead-lettered after {} attempts: {}",
                    saved.getId(), message.attempt(), ex.getMessage());
        }
    }

    /**
     * Exponential backoff: base * 2^(retryCount-1), capped at {@link #MAX_BACKOFF_MS}.
     * With a 5s base that yields 5s, 10s, 20s, ... between attempts.
     */
    private long backoffMs(int retryCount) {
        long delay = BASE_BACKOFF_MS * (1L << Math.max(0, retryCount - 1));
        return Math.min(delay, MAX_BACKOFF_MS);
    }

    /**
     * Simulated long-running work. Honours two optional payload flags:
     * {@code simulateFailure} (throws to exercise the failure path) and
     * {@code sleepMs} (controls how long the job runs; defaults to a random
     * 2-5 seconds).
     */
    private Map<String, Object> runJob(JobMessage message) throws InterruptedException {
        Map<String, Object> payload = message.payload();

        Thread.sleep(resolveSleepMs(payload));

        if (payload != null && Boolean.TRUE.equals(payload.get("simulateFailure"))) {
            throw new IllegalStateException("Simulated failure requested by payload");
        }

        return Map.of(
                "message", "Processed " + message.jobType(),
                "processedAt", Instant.now().toString());
    }

    private long resolveSleepMs(Map<String, Object> payload) {
        if (payload != null && payload.get("sleepMs") instanceof Number requested) {
            return Math.min(Math.max(requested.longValue(), 0), SLEEP_CAP_MS);
        }
        return ThreadLocalRandom.current().nextLong(MIN_SLEEP_MS, MAX_SLEEP_MS);
    }
}
