package com.queueforge.common.repository;

import com.queueforge.common.entity.Job;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

  Page<Job> findByStatus(JobStatus status, Pageable pageable);

  Page<Job> findByJobType(JobType jobType, Pageable pageable);

  Page<Job> findByStatusAndJobType(JobStatus status, JobType jobType, Pageable pageable);
}