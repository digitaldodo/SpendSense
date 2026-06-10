package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AffordabilityScenarioRequest(
        @NotNull @DecimalMin("1.00") BigDecimal purchaseAmount,
        @DecimalMin("0.00") BigDecimal downPayment,
        @NotNull @DecimalMin("0.00") BigDecimal annualInterestRate,
        @Min(1) @Max(360) int tenureMonths,
        @DecimalMin("0.00") BigDecimal existingMonthlyEmis,
        UUID goalId,
        String currency
) {
}
