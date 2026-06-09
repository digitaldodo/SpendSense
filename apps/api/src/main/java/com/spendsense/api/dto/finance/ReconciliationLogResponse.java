package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.ImportJobStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationLogResponse(
        UUID id,
        UUID importJobId,
        UUID accountId,
        ImportJobStatus status,
        int recordsSeen,
        int recordsImported,
        int recordsDuplicate,
        int recordsFailed,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal importedBalanceDelta,
        String metadataJson,
        Instant createdAt
) {
}
