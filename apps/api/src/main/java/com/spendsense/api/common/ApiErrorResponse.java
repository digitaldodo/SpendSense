package com.spendsense.api.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> details,
        String traceId,
        Instant timestamp
) {
    public static ApiErrorResponse of(
            String code,
            String message,
            Map<String, String> details,
            String traceId
    ) {
        return new ApiErrorResponse(false, code, message, details, traceId, Instant.now());
    }
}
