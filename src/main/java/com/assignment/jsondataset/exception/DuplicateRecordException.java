package com.assignment.jsondataset.exception;

/** Thrown when a record id already exists in the dataset (maps to HTTP 409). */
public class DuplicateRecordException extends RuntimeException {

    public DuplicateRecordException(String message) {
        super(message);
    }
}
