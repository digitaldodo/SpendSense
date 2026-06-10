package com.spendsense.api.controller;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.engagement.SystemStatusResponse;
import com.spendsense.api.service.delivery.WorkerObservabilityService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final WorkerObservabilityService workerObservabilityService;

    public HealthController(WorkerObservabilityService workerObservabilityService) {
        this.workerObservabilityService = workerObservabilityService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<Map<String, String>>> health(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP", "service", "spendsense-api"),
                "SpendSense API is healthy.",
                traceId
        ));
    }

    @GetMapping("/metrics")
    ResponseEntity<ApiResponse<SystemStatusResponse>> metrics(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                workerObservabilityService.systemStatus(),
                "SpendSense delivery metrics loaded.",
                traceId
        ));
    }
}
