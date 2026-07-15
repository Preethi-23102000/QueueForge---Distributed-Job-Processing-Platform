package com.queueforge.worker.processor;

import com.queueforge.common.entity.Job;
import com.queueforge.common.entity.JobExecution;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.repository.JobExecutionRepository;
import com.queueforge.common.repository.JobRepository;
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
 * execution attempt, and marks the job COMPLETED or FAILED.
 */
@Component
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final String workerId;

    public JobProcessor(JobRepository jobRepository, JobExecutionRepository executionRepository) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void process(JobMessage message) {
        Optional<Job> maybeJob = jobRepository.findById(message.jobId());
        if (maybeJob.isEmpty()) {
            log.warn("Received message for unknown job {}", message.jobId());
            return;
        }

        Job job = maybeJob.get();
        if (job.getStatus() != JobStatus.QUEUED) {
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
            job.setCompletedAt(completedAt);
            job = jobRepository.save(job);

            execution.setStatus(JobStatus.COMPLETED);
            execution.setCompletedAt(completedAt);
            execution.setDurationMs(durationMs);
            executionRepository.save(execution);

            log.info("Job {} completed in {} ms by {}", job.getId(), durationMs, workerId);
        } catch (Exception ex) {
            Instant completedAt = Instant.now();
            long durationMs = Duration.between(startedAt, completedAt).toMillis();

            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(completedAt);
            job = jobRepository.save(job);

            execution.setStatus(JobStatus.FAILED);
            execution.setCompletedAt(completedAt);
            execution.setDurationMs(durationMs);
            execution.setErrorMessage(ex.getMessage());
            executionRepository.save(execution);

            log.warn("Job {} failed after {} ms: {}", job.getId(), durationMs, ex.getMessage());
        }
    }

    private static final long MIN_SLEEP_MS = 2000;
    private static final long MAX_SLEEP_MS = 5000;
    private static final long SLEEP_CAP_MS = 300000;

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
