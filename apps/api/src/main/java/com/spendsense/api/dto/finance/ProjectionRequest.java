package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record ProjectionRequest(
        @Min(1) @Max(360) Integer months,
        @DecimalMin("0.00") BigDecimal monthlySavingsOverride,
        @DecimalMin("0.00") BigDecimal emergencyMonthlyExpenseOverride
) {
}
