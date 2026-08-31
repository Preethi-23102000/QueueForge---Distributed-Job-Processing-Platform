package com.queueforge.api.repository;

import com.queueforge.api.AbstractIntegrationTest;
import com.queueforge.common.entity.Job;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import com.queueforge.common.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the Flyway schema and JPA queries against a real Postgres, including
 * the unique idempotency-key constraint added in V2.
 */
@Transactional
class JobRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void savesAndFindsByStatus() {
        jobRepository.save(newJob(JobStatus.FAILED));
        jobRepository.save(newJob(JobStatus.QUEUED));

        assertThat(jobRepository.findByStatus(JobStatus.FAILED, PageRequest.of(0, 10)).getContent())
                .isNotEmpty()
                .allSatisfy(job -> assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED));
    }

    @Test
    void findsByIdempotencyKey() {
        Job job = newJob(JobStatus.QUEUED);
        job.setIdempotencyKey("key-1");
        jobRepository.save(job);

        assertThat(jobRepository.findByIdempotencyKey("key-1")).isPresent();
        assertThat(jobRepository.findByIdempotencyKey("missing")).isEmpty();
    }

    @Test
    void enforcesUniqueIdempotencyKey() {
        Job first = newJob(JobStatus.QUEUED);
        first.setIdempotencyKey("dup");
        jobRepository.saveAndFlush(first);

        Job second = newJob(JobStatus.QUEUED);
        second.setIdempotencyKey("dup");

        assertThatThrownBy(() -> jobRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Job newJob(JobStatus status) {
        Job job = new Job();
        job.setJobType(JobType.REPORT_GENERATION);
        job.setStatus(status);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        return job;
    }
}
