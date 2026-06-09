package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalContributionResponse(
        UUID id,
        BigDecimal amount,
        LocalDate contributedOn,
        String note,
        Instant createdAt
) {
}
