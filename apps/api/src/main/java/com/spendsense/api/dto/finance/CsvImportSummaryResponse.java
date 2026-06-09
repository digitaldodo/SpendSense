package com.spendsense.api.dto.finance;

import java.util.List;

public record CsvImportSummaryResponse(
        ImportJobResponse job,
        int recordsSeen,
        int recordsImported,
        int recordsDuplicate,
        int recordsFailed,
        List<ImportFailureResponse> failures
) {
}
