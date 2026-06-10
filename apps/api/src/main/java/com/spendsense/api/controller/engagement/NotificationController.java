package com.spendsense.api.controller.engagement;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.engagement.DeliveryHistoryResponse;
import com.spendsense.api.dto.engagement.DeliveryRetryResponse;
import com.spendsense.api.dto.engagement.EmailPreviewResponse;
import com.spendsense.api.dto.engagement.NotificationDashboardResponse;
import com.spendsense.api.dto.engagement.NotificationPreferenceRequest;
import com.spendsense.api.dto.engagement.NotificationPreferenceResponse;
import com.spendsense.api.dto.engagement.NotificationResponse;
import com.spendsense.api.dto.engagement.NotificationSummaryResponse;
import com.spendsense.api.dto.engagement.ReportDeliveryLogResponse;
import com.spendsense.api.dto.engagement.ScheduledReportRequest;
import com.spendsense.api.dto.engagement.ScheduledReportResponse;
import com.spendsense.api.dto.engagement.SystemStatusResponse;
import com.spendsense.api.dto.engagement.WorkerJobLogResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.delivery.DeliveryMonitoringService;
import com.spendsense.api.service.delivery.NotificationDeliveryService;
import com.spendsense.api.service.delivery.WorkerObservabilityService;
import com.spendsense.api.service.finance.NotificationEngagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationEngagementService notificationEngagementService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final DeliveryMonitoringService deliveryMonitoringService;
    private final WorkerObservabilityService workerObservabilityService;

    public NotificationController(
            NotificationEngagementService notificationEngagementService,
            NotificationDeliveryService notificationDeliveryService,
            DeliveryMonitoringService deliveryMonitoringService,
            WorkerObservabilityService workerObservabilityService
    ) {
        this.notificationEngagementService = notificationEngagementService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.deliveryMonitoringService = deliveryMonitoringService;
        this.workerObservabilityService = workerObservabilityService;
    }

    @GetMapping
    ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.list(principal, unreadOnly),
                "Notifications loaded.",
                traceId
        ));
    }

    @GetMapping("/summary")
    ResponseEntity<ApiResponse<NotificationSummaryResponse>> summary(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.summary(principal),
                "Notification summary loaded.",
                traceId
        ));
    }

    @GetMapping("/preferences")
    ResponseEntity<ApiResponse<NotificationPreferenceResponse>> preferences(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.preferences(principal),
                "Notification preferences loaded.",
                traceId
        ));
    }

    @PatchMapping("/preferences")
    ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestBody NotificationPreferenceRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.updatePreferences(principal, request),
                "Notification preferences updated.",
                traceId
        ));
    }

    @PatchMapping("/{notificationId}/read")
    ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID notificationId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.markRead(principal, notificationId),
                "Notification marked read.",
                traceId
        ));
    }

    @PatchMapping("/read-all")
    ResponseEntity<ApiResponse<Long>> markAllRead(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.markAllRead(principal),
                "Notifications marked read.",
                traceId
        ));
    }

    @DeleteMapping("/{notificationId}")
    ResponseEntity<ApiResponse<Void>> dismiss(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID notificationId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        notificationEngagementService.dismiss(principal, notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification dismissed.", traceId));
    }

    @GetMapping("/dashboard")
    ResponseEntity<ApiResponse<NotificationDashboardResponse>> dashboard(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.dashboardWidgets(principal),
                "Notification dashboard widgets loaded.",
                traceId
        ));
    }

    @GetMapping("/scheduled-reports")
    ResponseEntity<ApiResponse<List<ScheduledReportResponse>>> scheduledReports(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.scheduledReports(principal),
                "Scheduled reports loaded.",
                traceId
        ));
    }

    @PostMapping("/scheduled-reports")
    ResponseEntity<ApiResponse<ScheduledReportResponse>> createScheduledReport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody ScheduledReportRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.createScheduledReport(principal, request),
                "Scheduled report created.",
                traceId
        ));
    }

    @PatchMapping("/scheduled-reports/{scheduleId}")
    ResponseEntity<ApiResponse<ScheduledReportResponse>> updateScheduledReport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID scheduleId,
            @RequestBody ScheduledReportRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.updateScheduledReport(principal, scheduleId, request),
                "Scheduled report updated.",
                traceId
        ));
    }

    @DeleteMapping("/scheduled-reports/{scheduleId}")
    ResponseEntity<ApiResponse<Void>> deleteScheduledReport(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID scheduleId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        notificationEngagementService.deleteScheduledReport(principal, scheduleId);
        return ResponseEntity.ok(ApiResponse.success(null, "Scheduled report paused.", traceId));
    }

    @GetMapping("/scheduled-reports/delivery-logs")
    ResponseEntity<ApiResponse<List<ReportDeliveryLogResponse>>> deliveryLogs(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationEngagementService.deliveryLogs(principal),
                "Report delivery logs loaded.",
                traceId
        ));
    }

    @GetMapping("/deliveries")
    ResponseEntity<ApiResponse<List<DeliveryHistoryResponse>>> deliveries(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationDeliveryService.history(principal),
                "Delivery history loaded.",
                traceId
        ));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    ResponseEntity<ApiResponse<DeliveryHistoryResponse>> retryDelivery(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID deliveryId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationDeliveryService.retryNow(principal, deliveryId),
                "Delivery retry queued.",
                traceId
        ));
    }

    @GetMapping("/deliveries/{deliveryId}/retries")
    ResponseEntity<ApiResponse<List<DeliveryRetryResponse>>> deliveryRetries(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID deliveryId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationDeliveryService.retries(principal, deliveryId),
                "Delivery retry history loaded.",
                traceId
        ));
    }

    @GetMapping("/email-preview")
    ResponseEntity<ApiResponse<EmailPreviewResponse>> emailPreview(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestParam(defaultValue = "WEEKLY_SUMMARY") String templateType,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                deliveryMonitoringService.preview(principal, templateType),
                "Email preview generated.",
                traceId
        ));
    }

    @GetMapping("/system-status")
    ResponseEntity<ApiResponse<SystemStatusResponse>> systemStatus(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                workerObservabilityService.systemStatus(),
                "Delivery system status loaded.",
                traceId
        ));
    }

    @GetMapping("/worker-jobs")
    ResponseEntity<ApiResponse<List<WorkerJobLogResponse>>> workerJobs(
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                workerObservabilityService.recentJobs(20),
                "Worker job logs loaded.",
                traceId
        ));
    }
}
