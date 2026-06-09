package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BalanceCorrectionRequest(
        @NotNull
        BigDecimal correctedBalance,
        String reason
) {
}
