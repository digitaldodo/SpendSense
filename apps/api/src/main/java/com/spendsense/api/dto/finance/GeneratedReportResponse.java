package com.spendsense.api.dto.finance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GeneratedReportResponse(
        UUID reportId,
        String reportType,
        String format,
        Instant generatedAt,
        FinancialInsightsResponse insights,
        List<CategorySpendResponse> categoryBreakdown
) {
}
