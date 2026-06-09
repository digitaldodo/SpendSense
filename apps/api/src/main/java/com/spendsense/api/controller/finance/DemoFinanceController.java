package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.DemoSeedResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.DemoFinanceSeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoFinanceController {
    private final DemoFinanceSeedService demoFinanceSeedService;

    public DemoFinanceController(DemoFinanceSeedService demoFinanceSeedService) {
        this.demoFinanceSeedService = demoFinanceSeedService;
    }

    @PostMapping("/finance-seed")
    ResponseEntity<ApiResponse<DemoSeedResponse>> seedFinanceDemo(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                demoFinanceSeedService.seedForCurrentUser(principal),
                "Demo finance data seeded.",
                traceId
        ));
    }
}
