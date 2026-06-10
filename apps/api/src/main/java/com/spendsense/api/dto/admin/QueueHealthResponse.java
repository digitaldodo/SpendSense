package com.spendsense.api.dto.admin;

public record QueueHealthResponse(
        String queueName,
        long ready,
        long running,
        long retryScheduled,
        long deadLetter,
        long lagSeconds,
        long throughputLastHour,
        long failuresLastHour
) {
}
