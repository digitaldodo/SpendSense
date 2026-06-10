package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.GeneratedReportResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.ReportExportService;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @GetMapping("/monthly")
    ResponseEntity<ApiResponse<GeneratedReportResponse>> monthlyReport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportExportService.monthlyReport(principal, month), "Monthly report generated.", traceId));
    }

    @GetMapping("/exports/csv")
    ResponseEntity<byte[]> csvExport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type
    ) {
        ReportExportService.ExportFile export = reportExportService.csvExport(principal, from, to, type);
        return download(export);
    }

    @GetMapping("/exports/pdf")
    ResponseEntity<byte[]> pdfExport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        ReportExportService.ExportFile export = reportExportService.pdfExport(principal, month);
        return download(export);
    }

    private ResponseEntity<byte[]> download(ReportExportService.ExportFile export) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(export.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(export.filename()).build());
        return ResponseEntity.ok().headers(headers).body(export.bytes());
    }
}
