package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.ImportFailureSeverity;
import java.util.UUID;

public record ImportFailureResponse(
        UUID id,
        int rowNumber,
        String errorCode,
        String message,
        ImportFailureSeverity severity,
        String rawRowJson
) {
}
