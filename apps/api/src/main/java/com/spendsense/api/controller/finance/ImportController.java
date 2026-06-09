package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.CsvImportSummaryResponse;
import com.spendsense.api.dto.finance.CsvPreviewResponse;
import com.spendsense.api.dto.finance.ImportFailureResponse;
import com.spendsense.api.dto.finance.ImportJobResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.ingestion.CsvImportService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private final CsvImportService csvImportService;

    public ImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping(value = "/csv/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<CsvPreviewResponse>> previewCsv(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "mapping", required = false) String mapping,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                csvImportService.preview(principal, file, csvImportService.parseMapping(mapping)),
                "CSV preview ready.",
                traceId
        ));
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<CsvImportSummaryResponse>> importCsv(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "mapping", required = false) String mapping,
            @RequestParam(name = "accountId", required = false) UUID accountId,
            @RequestParam(name = "accountName", required = false) String accountName,
            @RequestParam(name = "institutionName", required = false) String institutionName,
            @RequestParam(name = "idempotencyKey", required = false) String idempotencyKey,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                csvImportService.importCsv(
                        principal,
                        file,
                        csvImportService.parseMapping(mapping),
                        accountId,
                        accountName,
                        institutionName,
                        idempotencyKey
                ),
                "CSV import completed.",
                traceId
        ));
    }

    @GetMapping
    ResponseEntity<ApiResponse<List<ImportJobResponse>>> history(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                csvImportService.history(principal),
                "Import history loaded.",
                traceId
        ));
    }

    @GetMapping("/{jobId}/failures")
    ResponseEntity<ApiResponse<List<ImportFailureResponse>>> failures(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID jobId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                csvImportService.failures(principal, jobId),
                "Import failures loaded.",
                traceId
        ));
    }
}
