package com.spendsense.api.dto.finance;

import java.util.List;

public record CsvPreviewResponse(
        String filename,
        String fileChecksum,
        String fileSignature,
        List<String> columns,
        CsvColumnMappingRequest mapping,
        int recordsSeen,
        int validRows,
        int failedRows,
        int duplicateRows,
        List<CsvPreviewRowResponse> previewRows,
        List<ImportFailureResponse> failures
) {
}
