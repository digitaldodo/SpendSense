package com.spendsense.api.controller;

import com.spendsense.api.common.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
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
}
