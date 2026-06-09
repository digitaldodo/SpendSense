package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.AccountStatus;
import com.spendsense.api.domain.finance.AccountType;
import com.spendsense.api.domain.finance.IngestionSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String displayName,
        String institutionName,
        AccountType accountType,
        String accountMask,
        String currency,
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        AccountStatus status,
        IngestionSource source,
        Instant connectedAt,
        Instant lastSyncedAt
) {
}
