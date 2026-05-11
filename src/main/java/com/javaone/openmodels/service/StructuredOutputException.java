package com.javaone.openmodels.service;

public class StructuredOutputException extends RuntimeException {

    public StructuredOutputException(String message) {
        super(message);
    }

    public StructuredOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
