package com.spendsense.api.dto.finance;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AccountMergeRequest(
        @NotNull
        UUID targetAccountId,
        String reason
) {
}
