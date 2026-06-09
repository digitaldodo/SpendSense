package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendResponse(
        UUID categoryId,
        String name,
        String colorToken,
        BigDecimal total,
        long transactionCount,
        BigDecimal share
) {
}
