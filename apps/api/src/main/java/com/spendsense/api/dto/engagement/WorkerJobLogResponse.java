package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record WorkerJobLogResponse(
        UUID id,
        String jobName,
        String jobType,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        int recordsScanned,
        int recordsSucceeded,
        int recordsFailed,
        Instant heartbeatAt,
        String errorMessage
) {
}
