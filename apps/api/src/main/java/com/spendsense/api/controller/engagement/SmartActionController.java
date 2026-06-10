package com.spendsense.api.controller.engagement;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.SmartActionResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.WeeklyCheckIn;
import com.spendsense.api.dto.engagement.SmartActionStateRequest;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.SmartActionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actions")
public class SmartActionController {
    private final SmartActionService smartActionService;

    public SmartActionController(SmartActionService smartActionService) {
        this.smartActionService = smartActionService;
    }

    @GetMapping("/dashboard")
    ResponseEntity<ApiResponse<SmartActionDashboardResponse>> dashboard(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(smartActionService.dashboard(principal), "Smart action dashboard loaded.", traceId));
    }

    @PostMapping("/{actionId}/complete")
    ResponseEntity<ApiResponse<SmartActionResponse>> complete(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID actionId,
            @RequestBody(required = false) SmartActionStateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(smartActionService.complete(principal, actionId, request), "Smart action completed.", traceId));
    }

    @PostMapping("/{actionId}/dismiss")
    ResponseEntity<ApiResponse<SmartActionResponse>> dismiss(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID actionId,
            @RequestBody(required = false) SmartActionStateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(smartActionService.dismiss(principal, actionId, request), "Smart action dismissed.", traceId));
    }

    @PostMapping("/{actionId}/snooze")
    ResponseEntity<ApiResponse<SmartActionResponse>> snooze(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID actionId,
            @RequestBody(required = false) SmartActionStateRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(smartActionService.snooze(principal, actionId, request), "Smart action snoozed.", traceId));
    }

    @PostMapping("/weekly-check-in/complete")
    ResponseEntity<ApiResponse<WeeklyCheckIn>> completeWeeklyCheckIn(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(smartActionService.completeWeeklyCheckIn(principal), "Weekly check-in completed.", traceId));
    }
}
