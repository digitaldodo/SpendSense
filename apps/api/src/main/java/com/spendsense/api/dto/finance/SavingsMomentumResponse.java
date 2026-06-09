package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record SavingsMomentumResponse(
        BigDecimal monthNetSavings,
        BigDecimal goalContributionsThisMonth,
        BigDecimal savingsRatio,
        String state
) {
}
