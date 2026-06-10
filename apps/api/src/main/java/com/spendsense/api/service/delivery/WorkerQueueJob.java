package com.spendsense.api.service.delivery;

import java.time.Instant;
import java.util.UUID;

public record WorkerQueueJob(
        UUID id,
        String queueName,
        String jobType,
        String status,
        int priority,
        Instant scheduledFor,
        int attemptCount,
        int maxAttempts,
        String payloadJson,
        String traceId
) {
}
