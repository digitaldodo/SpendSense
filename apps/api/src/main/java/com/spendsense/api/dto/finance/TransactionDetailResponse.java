package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDetailResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        TransactionDirection direction,
        TransactionStatus status,
        Instant occurredAt,
        Instant bookedAt,
        String merchantName,
        String merchantNormalized,
        String description,
        String reference,
        IngestionSource source,
        String sourceTransactionId,
        String idempotencyKey,
        String dedupeFingerprint,
        UUID ingestionSessionId,
        AccountResponse account,
        CategoryResponse category,
        Instant createdAt,
        Instant updatedAt
) {
}
