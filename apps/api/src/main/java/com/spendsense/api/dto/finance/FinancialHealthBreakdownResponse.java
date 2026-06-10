package com.spendsense.api.dto.finance;

import java.time.Instant;
import java.util.List;

public record FinancialHealthBreakdownResponse(
        Instant generatedAt,
        String state,
        int score,
        String headline,
        List<FinancialHealthIndicatorResponse> indicators,
        List<FinancialHealthTrendPointResponse> trendHistory
) {
}
