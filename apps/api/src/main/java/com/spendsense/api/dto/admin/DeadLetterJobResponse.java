package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterJobResponse(
        UUID id,
        UUID workerQueueId,
        String queueName,
        String jobType,
        String failedStatus,
        int attemptCount,
        String payloadJson,
        String failureCode,
        String failureMessage,
        String traceId,
        Instant exhaustedAt,
        Instant retriedFromDeadLetterAt,
        Instant createdAt
) {
}
