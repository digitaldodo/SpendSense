package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.TransactionDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record CsvPreviewRowResponse(
        int rowNumber,
        Map<String, String> raw,
        Instant occurredAt,
        BigDecimal amount,
        TransactionDirection direction,
        String merchantName,
        String description,
        String reference,
        boolean duplicate,
        String warning
) {
}
