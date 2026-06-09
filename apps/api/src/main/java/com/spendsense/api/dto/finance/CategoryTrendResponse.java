package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CategoryTrendResponse(
        UUID categoryId,
        String name,
        String colorToken,
        Instant periodStart,
        BigDecimal total
) {
}
