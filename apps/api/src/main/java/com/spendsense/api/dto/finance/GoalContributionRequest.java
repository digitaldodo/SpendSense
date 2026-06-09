package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate contributedOn,
        String note
) {
}
