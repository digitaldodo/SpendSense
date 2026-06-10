package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record WorkerQueueResponse(
        UUID id,
        String queueName,
        String jobType,
        String status,
        int priority,
        Instant scheduledFor,
        String lockedBy,
        Instant lockedUntil,
        int attemptCount,
        int maxAttempts,
        String payloadJson,
        String traceId,
        String lastErrorCode,
        String lastErrorMessage,
        Instant enqueuedAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        long latencyMs,
        Instant createdAt,
        Instant updatedAt
) {
}
