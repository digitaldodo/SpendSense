package com.spendsense.api.service.delivery;

import com.spendsense.api.service.finance.NotificationEngagementService;
import com.spendsense.api.config.SpendSenseProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class DeliveryWorkerService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryWorkerService.class);
    private final NotificationEngagementService notificationEngagementService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final DigestGenerationService digestGenerationService;
    private final WorkerObservabilityService observabilityService;
    private final WorkerQueueService workerQueueService;
    private final SpendSenseProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public DeliveryWorkerService(
            NotificationEngagementService notificationEngagementService,
            NotificationDeliveryService notificationDeliveryService,
            DigestGenerationService digestGenerationService,
            WorkerObservabilityService observabilityService,
            WorkerQueueService workerQueueService,
            SpendSenseProperties properties,
            JdbcTemplate jdbcTemplate
    ) {
        this.notificationEngagementService = notificationEngagementService;
        this.notificationDeliveryService = notificationDeliveryService;
        this.digestGenerationService = digestGenerationService;
        this.observabilityService = observabilityService;
        this.workerQueueService = workerQueueService;
        this.properties = properties;
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
            int bridgedAttempts = notificationDeliveryService.runPendingRetries(batchSize());
            ProcessingStats queueStats = processDeliveryQueue();
            int cleaned = workerQueueService.cleanupCompletedJobs();
            scanned = notificationEmails + digests + bridgedAttempts + queueStats.scanned();
            succeeded = notificationEmails + digests + bridgedAttempts + queueStats.succeeded();
            failed = queueStats.failed();
            observabilityService.recordMetric("delivery.worker.scanned", scanned, "count", "OK", Map.of("job", "delivery-worker"));
            observabilityService.recordMetric("worker.queue.cleaned", cleaned, "count", "OK", Map.of("queue", "delivery"));
            observabilityService.finishJob(jobId, "SUCCESS", scanned, succeeded, failed, null);
        } catch (RuntimeException exception) {
            failed = Math.max(1, scanned - succeeded);
            observabilityService.finishJob(jobId, "FAILED", scanned, succeeded, failed, exception.getMessage());
            throw exception;
        }
    }

    ProcessingStats processDeliveryQueue() {
        String workerId = "delivery-worker-%s".formatted(UUID.randomUUID());
        List<WorkerQueueJob> jobs = workerQueueService.claimDue("delivery", batchSize(), workerId);
        int succeeded = 0;
        int failed = 0;
        for (WorkerQueueJob job : jobs) {
            try {
                Map<String, Object> payload = workerQueueService.readPayload(job.payloadJson());
                UUID deliveryId = UUID.fromString(String.valueOf(payload.get("deliveryId")));
                NotificationDeliveryService.DeliveryAttemptResult result = notificationDeliveryService.attemptDelivery(deliveryId);
                if (result.delivered()) {
                    workerQueueService.complete(job.id());
                    succeeded++;
                } else if (result.terminal() || job.attemptCount() >= job.maxAttempts()) {
                    workerQueueService.deadLetter(job, result.errorCode(), result.errorMessage());
                    failed++;
                } else {
                    workerQueueService.retry(job, result.errorCode(), result.errorMessage());
                    failed++;
                }
            } catch (RuntimeException exception) {
                log.warn("worker_queue_job_failed jobId={} jobType={} error={}", job.id(), job.jobType(), exception.getMessage());
                if (job.attemptCount() >= job.maxAttempts()) {
                    workerQueueService.deadLetter(job, "WORKER_EXCEPTION", exception.getMessage());
                } else {
                    workerQueueService.retry(job, "WORKER_EXCEPTION", exception.getMessage());
                }
                failed++;
            }
        }
        observabilityService.recordQueueLag("delivery");
        return new ProcessingStats(jobs.size(), succeeded, failed);
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

    private int batchSize() {
        Integer batchSize = properties.delivery().worker().batchSize();
        return batchSize == null ? 100 : Math.max(1, batchSize);
    }

    record ProcessingStats(int scanned, int succeeded, int failed) {
    }
}
