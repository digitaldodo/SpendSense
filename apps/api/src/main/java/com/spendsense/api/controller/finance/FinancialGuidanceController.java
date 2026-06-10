package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.AffordabilityScenarioRequest;
import com.spendsense.api.dto.finance.AffordabilityScenarioResponse;
import com.spendsense.api.dto.finance.FinancialHealthBreakdownResponse;
import com.spendsense.api.dto.finance.ProjectionRequest;
import com.spendsense.api.dto.finance.ProjectionResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.FinancialGuidanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-guidance")
public class FinancialGuidanceController {
    private final FinancialGuidanceService financialGuidanceService;

    public FinancialGuidanceController(FinancialGuidanceService financialGuidanceService) {
        this.financialGuidanceService = financialGuidanceService;
    }

    @GetMapping("/health")
    ResponseEntity<ApiResponse<FinancialHealthBreakdownResponse>> health(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(financialGuidanceService.financialHealth(principal), "Financial health guidance loaded.", traceId));
    }

    @PostMapping("/affordability-scenarios")
    ResponseEntity<ApiResponse<AffordabilityScenarioResponse>> affordabilityScenario(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody AffordabilityScenarioRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(financialGuidanceService.simulateAffordability(principal, request), "Affordability scenario calculated.", traceId));
    }

    @PostMapping("/projections")
    ResponseEntity<ApiResponse<ProjectionResponse>> projection(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody ProjectionRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(financialGuidanceService.project(principal, request), "Projection calculated.", traceId));
    }
}
