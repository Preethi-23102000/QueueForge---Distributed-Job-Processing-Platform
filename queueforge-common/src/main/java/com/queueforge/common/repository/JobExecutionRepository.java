package com.queueforge.common.repository;

import com.queueforge.common.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

  List<JobExecution> findByJobIdOrderByAttemptNumberAsc(UUID jobId);
}