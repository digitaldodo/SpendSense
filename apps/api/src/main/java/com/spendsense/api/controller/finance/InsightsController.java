package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.BudgetRolloverResponse;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.FinancialInsightsService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightsController {
    private final FinancialInsightsService financialInsightsService;

    public InsightsController(FinancialInsightsService financialInsightsService) {
        this.financialInsightsService = financialInsightsService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<FinancialInsightsResponse>> insights(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(financialInsightsService.insights(principal, from, to), "Insights generated.", traceId));
    }

    @PostMapping("/rollovers/materialize")
    ResponseEntity<ApiResponse<List<BudgetRolloverResponse>>> materializeRollovers(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(financialInsightsService.materializeBudgetRollovers(principal), "Budget rollovers materialized.", traceId));
    }
}
