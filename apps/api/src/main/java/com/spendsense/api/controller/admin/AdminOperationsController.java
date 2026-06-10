package com.spendsense.api.controller.admin;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.admin.AdminAuditLogResponse;
import com.spendsense.api.dto.admin.AdminOperationsOverviewResponse;
import com.spendsense.api.dto.admin.AdminRetryRequest;
import com.spendsense.api.dto.admin.AlertAcknowledgmentRequest;
import com.spendsense.api.dto.admin.DeadLetterJobResponse;
import com.spendsense.api.dto.admin.DeliveryTimelineEventResponse;
import com.spendsense.api.dto.admin.IncidentLogResponse;
import com.spendsense.api.dto.admin.OperationalAlertResponse;
import com.spendsense.api.dto.admin.ProviderDeliveryEventResponse;
import com.spendsense.api.dto.admin.ProviderWebhookEventResponse;
import com.spendsense.api.dto.admin.ReliabilityOverviewResponse;
import com.spendsense.api.dto.admin.RunbookEntryResponse;
import com.spendsense.api.dto.admin.WorkerQueueResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.admin.AdminOperationsService;
import com.spendsense.api.service.admin.OperationalReliabilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController {
    private final AdminOperationsService adminOperationsService;
    private final OperationalReliabilityService operationalReliabilityService;

    public AdminOperationsController(
            AdminOperationsService adminOperationsService,
            OperationalReliabilityService operationalReliabilityService
    ) {
        this.adminOperationsService = adminOperationsService;
        this.operationalReliabilityService = operationalReliabilityService;
    }

    @GetMapping("/overview")
    ResponseEntity<ApiResponse<AdminOperationsOverviewResponse>> overview(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.overview(),
                "Admin operations overview loaded.",
                traceId
        ));
    }

    @GetMapping("/queues")
    ResponseEntity<ApiResponse<List<WorkerQueueResponse>>> queues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String queueName,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.queueJobs(status, queueName, search, limit),
                "Worker queue jobs loaded.",
                traceId
        ));
    }

    @GetMapping("/queues/{jobId}")
    ResponseEntity<ApiResponse<WorkerQueueResponse>> queueJob(
            @PathVariable UUID jobId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.queueJob(jobId),
                "Worker queue job loaded.",
                traceId
        ));
    }

    @PostMapping("/queues/{jobId}/retry")
    ResponseEntity<ApiResponse<WorkerQueueResponse>> retryQueueJob(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID jobId,
            @RequestBody(required = false) AdminRetryRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.retryQueueJob(principal, jobId, request == null ? null : request.reason(), traceId),
                "Queue job retry scheduled.",
                traceId
        ));
    }

    @GetMapping("/dead-letter")
    ResponseEntity<ApiResponse<List<DeadLetterJobResponse>>> deadLetters(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.deadLetters(search, limit),
                "Dead-letter jobs loaded.",
                traceId
        ));
    }

    @PostMapping("/dead-letter/{deadLetterId}/retry")
    ResponseEntity<ApiResponse<DeadLetterJobResponse>> retryDeadLetter(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID deadLetterId,
            @RequestBody(required = false) AdminRetryRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.retryDeadLetter(principal, deadLetterId, request == null ? null : request.reason(), traceId),
                "Dead-letter job retry scheduled.",
                traceId
        ));
    }

    @GetMapping("/provider-events")
    ResponseEntity<ApiResponse<List<ProviderDeliveryEventResponse>>> providerEvents(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.providerEvents(provider, status, limit),
                "Provider delivery events loaded.",
                traceId
        ));
    }

    @GetMapping("/audit-logs")
    ResponseEntity<ApiResponse<List<AdminAuditLogResponse>>> auditLogs(
            @RequestParam(defaultValue = "40") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminOperationsService.auditLogs(limit),
                "Admin audit logs loaded.",
                traceId
        ));
    }

    @GetMapping("/reliability/overview")
    ResponseEntity<ApiResponse<ReliabilityOverviewResponse>> reliabilityOverview(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.overview(),
                "Reliability overview loaded.",
                traceId
        ));
    }

    @GetMapping("/alerts")
    ResponseEntity<ApiResponse<List<OperationalAlertResponse>>> alerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.alerts(status, severity, limit),
                "Operational alerts loaded.",
                traceId
        ));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    ResponseEntity<ApiResponse<OperationalAlertResponse>> acknowledgeAlert(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID alertId,
            @RequestBody(required = false) AlertAcknowledgmentRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.acknowledgeAlert(principal, alertId, request, traceId),
                "Operational alert acknowledged.",
                traceId
        ));
    }

    @GetMapping("/incidents")
    ResponseEntity<ApiResponse<List<IncidentLogResponse>>> incidents(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "40") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.incidents(status, limit),
                "Incidents loaded.",
                traceId
        ));
    }

    @GetMapping("/incidents/{incidentId}")
    ResponseEntity<ApiResponse<IncidentLogResponse>> incident(
            @PathVariable UUID incidentId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.incident(incidentId),
                "Incident loaded.",
                traceId
        ));
    }

    @GetMapping("/runbooks")
    ResponseEntity<ApiResponse<List<RunbookEntryResponse>>> runbooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.runbooks(search, severity, category, limit),
                "Runbooks loaded.",
                traceId
        ));
    }

    @GetMapping("/runbooks/{slug}")
    ResponseEntity<ApiResponse<RunbookEntryResponse>> runbook(
            @PathVariable String slug,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.runbook(slug),
                "Runbook loaded.",
                traceId
        ));
    }

    @GetMapping("/webhook-events")
    ResponseEntity<ApiResponse<List<ProviderWebhookEventResponse>>> webhookEvents(
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "80") int limit,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.webhookEvents(provider, status, limit),
                "Provider webhook events loaded.",
                traceId
        ));
    }

    @GetMapping("/deliveries/{deliveryId}/timeline")
    ResponseEntity<ApiResponse<List<DeliveryTimelineEventResponse>>> deliveryTimeline(
            @PathVariable UUID deliveryId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                operationalReliabilityService.deliveryTimeline(deliveryId),
                "Delivery timeline loaded.",
                traceId
        ));
    }
}
