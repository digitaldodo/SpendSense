package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectionPointResponse(
        LocalDate monthStart,
        BigDecimal projectedBalance,
        BigDecimal cumulativeSavings,
        BigDecimal emergencyRunwayMonths
) {
}
