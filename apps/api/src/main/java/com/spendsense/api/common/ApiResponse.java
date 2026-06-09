package com.spendsense.api.common;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String traceId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message, String traceId) {
        return new ApiResponse<>(true, data, message, traceId, Instant.now());
    }
}
