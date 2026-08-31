package com.queueforge.api.service;

import com.queueforge.api.exception.InvalidJobStateException;
import com.queueforge.api.exception.JobNotFoundException;
import com.queueforge.api.messaging.JobQueuedEvent;
import com.queueforge.common.dto.JobRequest;
import com.queueforge.common.dto.JobResponse;
import com.queueforge.common.entity.Job;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import com.queueforge.common.repository.JobExecutionRepository;
import com.queueforge.common.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository executionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private JobService jobService;

    @Test
    void submit_persistsQueuedJobAndPublishesEvent() {
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        JobResponse response = jobService.submit(
                new JobRequest(JobType.REPORT_GENERATION, Map.of("k", "v"), 3), null);

        assertThat(response.status()).isEqualTo(JobStatus.QUEUED);
        assertThat(response.jobId()).isNotNull();
        verify(eventPublisher).publishEvent(any(JobQueuedEvent.class));
    }

    @Test
    void submit_withExistingIdempotencyKey_returnsExistingJobWithoutSaving() {
        Job existing = jobWith(JobStatus.COMPLETED);
        when(jobRepository.findByIdempotencyKey("abc")).thenReturn(Optional.of(existing));

        JobResponse response = jobService.submit(
                new JobRequest(JobType.REPORT_GENERATION, null, null), "abc");

        assertThat(response.jobId()).isEqualTo(existing.getId());
        verify(jobRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void get_missingJob_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.get(id))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void retry_nonFailedJob_isRejected() {
        Job job = jobWith(JobStatus.QUEUED);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.retry(job.getId()))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void retry_failedJobWithinLimit_incrementsAndRequeues() {
        Job job = jobWith(JobStatus.FAILED);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.retry(job.getId());

        assertThat(response.status()).isEqualTo(JobStatus.QUEUED);
        assertThat(response.retryCount()).isEqualTo(1);
        verify(eventPublisher).publishEvent(any(JobQueuedEvent.class));
    }

    @Test
    void retry_failedJobWithRetriesExhausted_isRejected() {
        Job job = jobWith(JobStatus.FAILED);
        job.setRetryCount(3);
        job.setMaxRetries(3);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.retry(job.getId()))
                .isInstanceOf(InvalidJobStateException.class);
    }

    @Test
    void cancel_queuedJob_setsCancelled() {
        Job job = jobWith(JobStatus.QUEUED);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.cancel(job.getId());

        assertThat(response.status()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancel_jobAlreadyProcessing_isRejected() {
        Job job = jobWith(JobStatus.PROCESSING);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.cancel(job.getId()))
                .isInstanceOf(InvalidJobStateException.class);
    }

    private Job jobWith(JobStatus status) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.REPORT_GENERATION);
        job.setStatus(status);
        job.setMaxRetries(3);
        return job;
    }
}
