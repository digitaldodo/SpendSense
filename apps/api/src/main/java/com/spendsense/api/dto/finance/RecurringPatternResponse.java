package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringPatternResponse(
        UUID categoryId,
        String categoryName,
        String merchantName,
        String merchantNormalized,
        BigDecimal amount,
        String currency,
        String cadence,
        int occurrenceCount,
        LocalDate firstSeenOn,
        LocalDate lastSeenOn,
        LocalDate nextExpectedOn,
        BigDecimal confidence,
        boolean subscription
) {
}
