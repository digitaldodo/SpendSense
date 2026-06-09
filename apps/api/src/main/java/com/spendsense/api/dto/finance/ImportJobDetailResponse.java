package com.spendsense.api.dto.finance;

import java.util.List;

public record ImportJobDetailResponse(
        ImportJobResponse job,
        CsvColumnMappingRequest mapping,
        String summaryJson,
        String reconciliationMetadataJson,
        List<ImportFailureResponse> failures,
        List<ReconciliationLogResponse> reconciliationLogs
) {
}
