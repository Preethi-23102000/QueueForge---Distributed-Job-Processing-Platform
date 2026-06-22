package com.queueforge.common.enums;

/**
 * Lifecycle states a job moves through.
 * Stage 1 uses QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED.
 * RETRYING and DEAD_LETTERED come into play in Stage 2 (reliability).
 */
public enum JobStatus {
  QUEUED,
  PROCESSING,
  COMPLETED,
  FAILED,
  RETRYING,
  CANCELLED,
  DEAD_LETTERED
}