-- Idempotent submission: an optional client-supplied key that de-duplicates jobs.
ALTER TABLE jobs ADD COLUMN idempotency_key VARCHAR(255);

-- Partial unique index: at most one job per key, but many jobs may have no key (NULL).
CREATE UNIQUE INDEX uq_jobs_idempotency_key
    ON jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
