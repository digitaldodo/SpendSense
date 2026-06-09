package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.IngestionSource;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.domain.finance.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
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
        AccountResponse account,
        CategoryResponse category
) {
}
