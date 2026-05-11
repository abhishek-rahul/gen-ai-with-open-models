package com.javaone.openmodels.dto;

public record StructuredResponse<T>(
    T data,
    String rawJson,
    int attempts
) {
}
