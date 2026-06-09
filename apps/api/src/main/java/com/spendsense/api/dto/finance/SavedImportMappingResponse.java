package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.IngestionSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SavedImportMappingResponse(
        UUID id,
        IngestionSource source,
        String name,
        String fileSignature,
        CsvColumnMappingRequest mapping,
        BigDecimal confidenceScore,
        int useCount,
        Instant lastUsedAt
) {
}
