package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String currency,
        LocalDate startsOn,
        Boolean rolloverEnabled,
        String reason
) {
}
