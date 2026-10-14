package com.assignment.jsondataset.exception;

/** Thrown when the requested dataset has no records (maps to HTTP 404). */
public class DatasetNotFoundException extends RuntimeException {

    public DatasetNotFoundException(String message) {
        super(message);
    }
}
