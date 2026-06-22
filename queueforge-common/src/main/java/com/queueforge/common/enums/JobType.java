package com.queueforge.common.enums;

/**
 * Kinds of work the platform can run.
 * Start small — REPORT_GENERATION is the simulated long-running job for Stage 1.
 */
public enum JobType {
  REPORT_GENERATION,
  CSV_VALIDATION
}