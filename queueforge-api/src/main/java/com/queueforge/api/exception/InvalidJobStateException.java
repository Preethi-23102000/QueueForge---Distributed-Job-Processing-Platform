package com.queueforge.api.exception;

/**
 * Thrown when an operation is not allowed for a job's current status
 * (for example, retrying a job that has not failed).
 */
public class InvalidJobStateException extends RuntimeException {

    public InvalidJobStateException(String message) {
        super(message);
    }
}
