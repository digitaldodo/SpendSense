package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.ImportJobStatus;
import com.spendsense.api.domain.finance.IngestionSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ImportJobResponse(
        UUID id,
        IngestionSource source,
        ImportJobStatus status,
        String originalFilename,
        String fileChecksum,
        String idempotencyKey,
        int recordsSeen,
        int recordsImported,
        int recordsDuplicate,
        int recordsFailed,
        BigDecimal mappingConfidenceScore,
        Instant startedAt,
        Instant completedAt,
        AccountResponse account
) {
}
