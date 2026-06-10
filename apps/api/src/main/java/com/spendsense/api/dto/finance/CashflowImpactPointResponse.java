package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashflowImpactPointResponse(
        LocalDate monthStart,
        BigDecimal baselineFreeCashflow,
        BigDecimal simulatedFreeCashflow,
        BigDecimal projectedSavingsBalance
) {
}
