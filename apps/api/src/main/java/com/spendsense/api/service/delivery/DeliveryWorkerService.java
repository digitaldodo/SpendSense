package com.spendsense.api.service.delivery;

import com.spendsense.api.service.finance.NotificationEngagementService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class DeliveryWorkerService {
    private final NotificationEngagementService notificationEngagementService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final DigestGenerationService digestGenerationService;
    private final WorkerObservabilityService observabilityService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public DeliveryWorkerService(
            NotificationEngagementService notificationEngagementService,
            NotificationDeliveryService notificationDeliveryService,
            DigestGenerationService digestGenerationService,
            WorkerObservabilityService observabilityService,
            JdbcTemplate jdbcTemplate
    ) {
        this.notificationEngagementService = notificationEngagementService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.digestGenerationService = digestGenerationService;
        this.observabilityService = observabilityService;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = Clock.systemUTC();
    }

    @Scheduled(fixedDelayString = "${spendsense.delivery.worker-delay-ms:300000}")
    @Transactional
    public void runDeliveryWorker() {
        UUID jobId = observabilityService.startJob("delivery-worker", "SCHEDULED_DELIVERY", Map.of("version", "phase-10"));
        int scanned = 0;
        int succeeded = 0;
        int failed = 0;
        try {
            notificationEngagementService.runScheduledEngagementJobs();
            observabilityService.heartbeat(jobId);
            int notificationEmails = notificationDeliveryService.dispatchNotificationEmailCandidates(100);
            int digests = queueDueDigests(100);
            int attempts = notificationDeliveryService.runPendingRetries(150);
            scanned = notificationEmails + digests + attempts;
            succeeded = scanned;
            observabilityService.recordMetric("delivery.worker.scanned", scanned, "count", "OK", Map.of("job", "delivery-worker"));
            observabilityService.finishJob(jobId, "SUCCESS", scanned, succeeded, failed, null);
        } catch (RuntimeException exception) {
            failed = Math.max(1, scanned - succeeded);
            observabilityService.finishJob(jobId, "FAILED", scanned, succeeded, failed, exception.getMessage());
            throw exception;
        }
    }

    int queueDueDigests(int limit) {
        LocalDate today = LocalDate.now(clock);
        Instant weeklyCutoff = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant monthlyCutoff = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<DigestCandidate> weekly = jdbcTemplate.query("""
                select user_profile_id from notification_preferences
                where email_enabled = true
                  and (weekly_digest_enabled = true or digest_frequency = 'WEEKLY')
                  and not exists (
                    select 1 from notification_deliveries d
                    where d.user_profile_id = notification_preferences.user_profile_id
                      and d.delivery_kind = 'WEEKLY_SUMMARY'
                      and d.created_at >= ?
                  )
                limit ?
                """, (rs, rowNum) -> new DigestCandidate(rs.getObject("user_profile_id", UUID.class), "WEEKLY_SUMMARY"), weeklyCutoff, limit);
        List<DigestCandidate> monthly = jdbcTemplate.query("""
                select user_profile_id from notification_preferences
                where email_enabled = true
                  and (monthly_report_enabled = true or digest_frequency = 'MONTHLY')
                  and not exists (
                    select 1 from notification_deliveries d
                    where d.user_profile_id = notification_preferences.user_profile_id
                      and d.delivery_kind = 'MONTHLY_FINANCIAL_SUMMARY'
                      and d.created_at >= ?
                  )
                limit ?
                """, (rs, rowNum) -> new DigestCandidate(rs.getObject("user_profile_id", UUID.class), "MONTHLY_FINANCIAL_SUMMARY"), monthlyCutoff, limit);
        int queued = 0;
        for (DigestCandidate candidate : weekly) {
            notificationDeliveryService.queueEmail(
                    candidate.userProfileId(),
                    null,
                    null,
                    null,
                    candidate.kind(),
                    digestGenerationService.weeklySummary(candidate.userProfileId()),
                    null
            );
            queued++;
        }
        for (DigestCandidate candidate : monthly) {
            notificationDeliveryService.queueEmail(
                    candidate.userProfileId(),
                    null,
                    null,
                    null,
                    candidate.kind(),
                    digestGenerationService.monthlySummary(candidate.userProfileId()),
                    null
            );
            queued++;
        }
        return queued;
    }

    private record DigestCandidate(UUID userProfileId, String kind) {
    }
}
