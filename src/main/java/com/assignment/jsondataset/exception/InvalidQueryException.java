package com.assignment.jsondataset.exception;

/** Thrown when a request is syntactically valid but semantically wrong (maps to HTTP 400). */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
