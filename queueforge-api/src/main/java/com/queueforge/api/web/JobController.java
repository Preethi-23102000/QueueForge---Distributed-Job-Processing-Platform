package com.queueforge.api.web;

import com.queueforge.api.service.JobService;
import com.queueforge.common.dto.JobExecutionResponse;
import com.queueforge.common.dto.JobRequest;
import com.queueforge.common.dto.JobResponse;
import com.queueforge.common.enums.JobStatus;
import com.queueforge.common.enums.JobType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for submitting and tracking jobs.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public JobResponse submit(@Valid @RequestBody JobRequest request) {
        return jobService.submit(request);
    }

    @GetMapping("/{jobId}")
    public JobResponse get(@PathVariable UUID jobId) {
        return jobService.get(jobId);
    }

    @GetMapping
    public Page<JobResponse> list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType jobType,
            @PageableDefault(size = 20) Pageable pageable) {
        return jobService.list(status, jobType, pageable);
    }

    @GetMapping("/{jobId}/executions")
    public List<JobExecutionResponse> executions(@PathVariable UUID jobId) {
        return jobService.getExecutions(jobId);
    }

    @PostMapping("/{jobId}/retry")
    public JobResponse retry(@PathVariable UUID jobId) {
        return jobService.retry(jobId);
    }

    @PostMapping("/{jobId}/cancel")
    public JobResponse cancel(@PathVariable UUID jobId) {
        return jobService.cancel(jobId);
    }
}
