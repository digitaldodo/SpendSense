package com.spendsense.api.controller;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.engagement.SystemStatusResponse;
import com.spendsense.api.dto.ops.DeploymentHealthResponse;
import com.spendsense.api.dto.ops.ReleaseMetadataResponse;
import com.spendsense.api.service.delivery.WorkerObservabilityService;
import com.spendsense.api.service.ops.DeploymentHealthService;
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
    private final DeploymentHealthService deploymentHealthService;

    public HealthController(
            WorkerObservabilityService workerObservabilityService,
            DeploymentHealthService deploymentHealthService
    ) {
        this.workerObservabilityService = workerObservabilityService;
        this.deploymentHealthService = deploymentHealthService;
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

    @GetMapping("/live")
    ResponseEntity<ApiResponse<DeploymentHealthResponse>> liveness(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                deploymentHealthService.liveness(),
                "SpendSense API liveness confirmed.",
                traceId
        ));
    }

    @GetMapping("/heartbeat")
    ResponseEntity<ApiResponse<DeploymentHealthResponse>> heartbeat(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return liveness(traceId);
    }

    @GetMapping("/ready")
    ResponseEntity<ApiResponse<DeploymentHealthResponse>> readiness(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        DeploymentHealthResponse readiness = deploymentHealthService.readiness();
        return ResponseEntity.status("UP".equals(readiness.status()) ? 200 : 503).body(ApiResponse.success(
                readiness,
                "SpendSense API readiness checked.",
                traceId
        ));
    }

    @GetMapping("/deployment")
    ResponseEntity<ApiResponse<DeploymentHealthResponse>> deployment(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return readiness(traceId);
    }

    @GetMapping("/dependencies")
    ResponseEntity<ApiResponse<DeploymentHealthResponse>> dependencies(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return readiness(traceId);
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

    @GetMapping("/version")
    ResponseEntity<ApiResponse<ReleaseMetadataResponse>> version(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                deploymentHealthService.releaseMetadata(),
                "SpendSense API release metadata loaded.",
                traceId
        ));
    }
}
