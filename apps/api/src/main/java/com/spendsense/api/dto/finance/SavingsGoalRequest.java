package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.SavingsGoalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
        @DecimalMin(value = "0.00") BigDecimal currentAmount,
        String currency,
        LocalDate targetDate,
        SavingsGoalStatus status,
        String colorToken,
        String iconName
) {
}
