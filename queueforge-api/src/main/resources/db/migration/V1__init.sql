-- Core job table: holds the latest state of each submitted job
CREATE TABLE jobs (
                      id            UUID PRIMARY KEY,
                      job_type      VARCHAR(64)  NOT NULL,
                      status        VARCHAR(32)  NOT NULL,
                      payload       JSONB,
                      result        JSONB,
                      error_message TEXT,
                      retry_count   INTEGER      NOT NULL DEFAULT 0,
                      max_retries   INTEGER      NOT NULL DEFAULT 3,
                      requested_by  VARCHAR(255),
                      created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      updated_at    TIMESTAMPTZ,
                      started_at    TIMESTAMPTZ,
                      completed_at  TIMESTAMPTZ,
                      version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_jobs_status     ON jobs (status);
CREATE INDEX idx_jobs_job_type   ON jobs (job_type);
CREATE INDEX idx_jobs_created_at ON jobs (created_at);

-- Per-attempt execution history: one row per processing attempt of a job
CREATE TABLE job_executions (
                                id             UUID PRIMARY KEY,
                                job_id         UUID         NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
                                attempt_number INTEGER      NOT NULL,
                                status         VARCHAR(32)  NOT NULL,
                                worker_id      VARCHAR(255),
                                started_at     TIMESTAMPTZ,
                                completed_at   TIMESTAMPTZ,
                                duration_ms    BIGINT,
                                error_message  TEXT
);

CREATE INDEX idx_job_executions_job_id ON job_executions (job_id);