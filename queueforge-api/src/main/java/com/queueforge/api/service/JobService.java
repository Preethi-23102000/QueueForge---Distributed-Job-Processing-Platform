package com.queueforge.api.service;

import com.queueforge.api.exception.InvalidJobStateException;
import com.queueforge.api.exception.JobNotFoundException;
import com.queueforge.api.messaging.JobQueuedEvent;
import com.queueforge.common.dto.JobExecutionResponse;
import com.queueforge.common.dto.JobRequest;
import com.queueforge.common.dto.JobResponse;
import com.queueforge.common.entity.Job;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import com.queueforge.common.messaging.JobMessage;
import com.queueforge.common.repository.JobExecutionRepository;
import com.queueforge.common.repository.JobRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core job operations: submission, lookup, listing, retry and cancellation.
 */
@Service
public class JobService {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JobService(JobRepository jobRepository,
                      JobExecutionRepository executionRepository,
                      ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Persists a new job in QUEUED state and publishes it for processing.
     *
     * <p>If an idempotency key is supplied and a job already exists for it, the
     * existing job is returned unchanged instead of creating a duplicate.
     */
    @Transactional
    public JobResponse submit(JobRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Job> existing = jobRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return JobMapper.toResponse(existing.get());
            }
        }

        Job job = new Job();
        job.setJobType(request.jobType());
        job.setStatus(JobStatus.QUEUED);
        job.setPayload(request.payload());
        job.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : DEFAULT_MAX_RETRIES);
        job.setRetryCount(0);
        job.setIdempotencyKey(
                (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null);

        Job saved = jobRepository.save(job);

        eventPublisher.publishEvent(new JobQueuedEvent(new JobMessage(
                saved.getId(),
                saved.getJobType(),
                1,
                saved.getPayload(),
                Instant.now())));

        return JobMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID jobId) {
        return JobMapper.toResponse(findJob(jobId));
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> list(JobStatus status, JobType jobType, Pageable pageable) {
        Page<Job> jobs;
        if (status != null && jobType != null) {
            jobs = jobRepository.findByStatusAndJobType(status, jobType, pageable);
        } else if (status != null) {
            jobs = jobRepository.findByStatus(status, pageable);
        } else if (jobType != null) {
            jobs = jobRepository.findByJobType(jobType, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }
        return jobs.map(JobMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getExecutions(UUID jobId) {
        findJob(jobId);
        return executionRepository.findByJobIdOrderByAttemptNumberAsc(jobId).stream()
                .map(JobMapper::toResponse)
                .toList();
    }

    /**
     * Re-queues a failed job for another processing attempt, until the job has
     * used up its allowed retries.
     */
    @Transactional
    public JobResponse retry(UUID jobId) {
        Job job = findJob(jobId);
        if (job.getStatus() != JobStatus.FAILED) {
            throw new InvalidJobStateException(
                    "Only FAILED jobs can be retried; job " + jobId + " is " + job.getStatus());
        }
        if (job.getRetryCount() >= job.getMaxRetries()) {
            throw new InvalidJobStateException(
                    "Job " + jobId + " has exhausted its retries (" + job.getMaxRetries() + ")");
        }

        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(null);
        job.setStatus(JobStatus.QUEUED);
        Job saved = jobRepository.save(job);

        eventPublisher.publishEvent(new JobQueuedEvent(new JobMessage(
                saved.getId(),
                saved.getJobType(),
                saved.getRetryCount() + 1,
                saved.getPayload(),
                Instant.now())));

        return JobMapper.toResponse(saved);
    }

    /**
     * Cancels a job that has not started processing yet.
     */
    @Transactional
    public JobResponse cancel(UUID jobId) {
        Job job = findJob(jobId);
        if (job.getStatus() != JobStatus.QUEUED) {
            throw new InvalidJobStateException(
                    "Only QUEUED jobs can be cancelled; job " + jobId + " is " + job.getStatus());
        }
        job.setStatus(JobStatus.CANCELLED);
        return JobMapper.toResponse(jobRepository.save(job));
    }

    private Job findJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }
}
