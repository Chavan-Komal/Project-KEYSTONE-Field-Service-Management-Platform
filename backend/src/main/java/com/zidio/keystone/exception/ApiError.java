package com.zidio.keystone.exception;

import java.time.Instant;
import java.util.List;

/** Consistent error shape for every failure response — Section 10.1 of the brief. */
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, fieldErrors);
    }
}
