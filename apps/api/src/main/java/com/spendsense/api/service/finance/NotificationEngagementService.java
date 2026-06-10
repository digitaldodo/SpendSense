package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.engagement.NotificationDashboardResponse;
import com.spendsense.api.dto.engagement.NotificationPreferenceRequest;
import com.spendsense.api.dto.engagement.NotificationPreferenceResponse;
import com.spendsense.api.dto.engagement.NotificationResponse;
import com.spendsense.api.dto.engagement.NotificationSummaryResponse;
import com.spendsense.api.dto.engagement.ReportDeliveryLogResponse;
import com.spendsense.api.dto.engagement.ScheduledReportRequest;
import com.spendsense.api.dto.engagement.ScheduledReportResponse;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.dto.finance.SavingsTrajectoryResponse;
import com.spendsense.api.dto.finance.SpendingAnomalyResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEngagementService {
    private final UserProfileSyncService userProfileSyncService;
    private final FinancialInsightsService financialInsightsService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationEngagementService(
            UserProfileSyncService userProfileSyncService,
            FinancialInsightsService financialInsightsService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.financialInsightsService = financialInsightsService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public NotificationSummaryResponse summary(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        generateDeterministicNotifications(userProfileId);
        long unread = countUnread(userProfileId);
        long active = countActive(userProfileId);
        return new NotificationSummaryResponse(
                unread,
                active,
                latest(userProfileId, 8),
                timeline(userProfileId, 30)
        );
    }

    @Transactional
    public List<NotificationResponse> list(SupabasePrincipal principal, Boolean unreadOnly) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        generateDeterministicNotifications(userProfileId);
        if (Boolean.TRUE.equals(unreadOnly)) {
            return jdbcTemplate.query("""
                    select * from notifications
                    where user_profile_id = ? and lifecycle_status = 'ACTIVE' and read_at is null
                    order by priority asc, created_at desc
                    limit 80
                    """, this::notificationRow, userProfileId);
        }
        return timeline(userProfileId, 80);
    }

    @Transactional
    public NotificationDashboardResponse dashboardWidgets(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return dashboardWidgets(userProfileId);
    }

    @Transactional
    public NotificationDashboardResponse dashboardWidgets(UUID userProfileId) {
        generateDeterministicNotifications(userProfileId);
        return new NotificationDashboardResponse(
                countUnread(userProfileId),
                notificationsByTypes(userProfileId, List.of("RECURRING_PAYMENT_DUE"), 4),
                notificationsByTypes(userProfileId, List.of("BUDGET_NEARING_LIMIT", "BUDGET_EXCEEDED"), 4),
                notificationsByTypes(userProfileId, List.of("REPORT_READY", "WEEKLY_SUMMARY_READY"), 3),
                scheduledReports(userProfileId),
                notificationsByTypes(userProfileId, List.of("SAVINGS_DECLINE", "SPENDING_INCREASE"), 3)
        );
    }

    @Transactional
    public NotificationPreferenceResponse preferences(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        ensurePreferences(userProfileId);
        return preference(userProfileId);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(SupabasePrincipal principal, NotificationPreferenceRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        ensurePreferences(userProfileId);
        NotificationPreferenceResponse current = preference(userProfileId);
        jdbcTemplate.update("""
                update notification_preferences
                set in_app_enabled = ?, budget_warnings_enabled = ?, recurring_reminders_enabled = ?,
                    report_ready_enabled = ?, savings_nudges_enabled = ?, spending_increase_enabled = ?,
                    weekly_digest_enabled = ?, monthly_report_enabled = ?, timezone = ?,
                    quiet_hours_start = ?, quiet_hours_end = ?, updated_at = current_timestamp
                where user_profile_id = ?
                """,
                value(request.inAppEnabled(), current.inAppEnabled()),
                value(request.budgetWarningsEnabled(), current.budgetWarningsEnabled()),
                value(request.recurringRemindersEnabled(), current.recurringRemindersEnabled()),
                value(request.reportReadyEnabled(), current.reportReadyEnabled()),
                value(request.savingsNudgesEnabled(), current.savingsNudgesEnabled()),
                value(request.spendingIncreaseEnabled(), current.spendingIncreaseEnabled()),
                value(request.weeklyDigestEnabled(), current.weeklyDigestEnabled()),
                value(request.monthlyReportEnabled(), current.monthlyReportEnabled()),
                request.timezone() == null || request.timezone().isBlank() ? current.timezone() : request.timezone(),
                request.quietHoursStart(),
                request.quietHoursEnd(),
                userProfileId
        );
        return preference(userProfileId);
    }

    @Transactional
    public NotificationResponse markRead(SupabasePrincipal principal, UUID notificationId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        jdbcTemplate.update("""
                update notifications
                set read_at = coalesce(read_at, current_timestamp), updated_at = current_timestamp
                where id = ? and user_profile_id = ?
                """, notificationId, userProfileId);
        return jdbcTemplate.queryForObject("select * from notifications where id = ? and user_profile_id = ?", this::notificationRow, notificationId, userProfileId);
    }

    @Transactional
    public long markAllRead(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return jdbcTemplate.update("""
                update notifications
                set read_at = coalesce(read_at, current_timestamp), updated_at = current_timestamp
                where user_profile_id = ? and lifecycle_status = 'ACTIVE' and read_at is null
                """, userProfileId);
    }

    @Transactional
    public void dismiss(SupabasePrincipal principal, UUID notificationId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        jdbcTemplate.update("""
                update notifications
                set lifecycle_status = 'DISMISSED', dismissed_at = current_timestamp, updated_at = current_timestamp
                where id = ? and user_profile_id = ?
                """, notificationId, userProfileId);
    }

    @Transactional
    public List<ScheduledReportResponse> scheduledReports(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return scheduledReports(userProfileId);
    }

    @Transactional
    public ScheduledReportResponse createScheduledReport(SupabasePrincipal principal, ScheduledReportRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        UUID id = UUID.randomUUID();
        String timezone = request.timezone() == null || request.timezone().isBlank() ? "Asia/Kolkata" : request.timezone();
        String cadence = normalizeCadence(request.cadence());
        String reportType = normalizeReportType(request.reportType());
        String format = normalizeFormat(request.format());
        Instant nextRunAt = nextRun(cadence, timezone, Instant.now(clock));
        jdbcTemplate.update("""
                insert into scheduled_reports (
                    id, user_profile_id, report_type, format, cadence, timezone, delivery_channel,
                    next_run_at, active, metadata_json, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                id,
                userProfileId,
                reportType,
                format,
                cadence,
                timezone,
                normalizeChannel(request.deliveryChannel()),
                nextRunAt,
                request.active() == null || request.active(),
                writeJson(Map.of("futureDeliveryReady", true, "deterministic", true))
        );
        return scheduledReport(id, userProfileId);
    }

    @Transactional
    public ScheduledReportResponse updateScheduledReport(SupabasePrincipal principal, UUID scheduleId, ScheduledReportRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        ScheduledReportResponse current = scheduledReport(scheduleId, userProfileId);
        String timezone = request.timezone() == null || request.timezone().isBlank() ? current.timezone() : request.timezone();
        String cadence = request.cadence() == null || request.cadence().isBlank() ? current.cadence() : normalizeCadence(request.cadence());
        jdbcTemplate.update("""
                update scheduled_reports
                set report_type = ?, format = ?, cadence = ?, timezone = ?, delivery_channel = ?,
                    next_run_at = ?, active = ?, updated_at = current_timestamp
                where id = ? and user_profile_id = ?
                """,
                request.reportType() == null || request.reportType().isBlank() ? current.reportType() : normalizeReportType(request.reportType()),
                request.format() == null || request.format().isBlank() ? current.format() : normalizeFormat(request.format()),
                cadence,
                timezone,
                request.deliveryChannel() == null || request.deliveryChannel().isBlank() ? current.deliveryChannel() : normalizeChannel(request.deliveryChannel()),
                nextRun(cadence, timezone, Instant.now(clock)),
                request.active() == null ? current.active() : request.active(),
                scheduleId,
                userProfileId
        );
        return scheduledReport(scheduleId, userProfileId);
    }

    @Transactional
    public void deleteScheduledReport(SupabasePrincipal principal, UUID scheduleId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        jdbcTemplate.update("""
                update scheduled_reports
                set active = false, updated_at = current_timestamp
                where id = ? and user_profile_id = ?
                """, scheduleId, userProfileId);
    }

    @Transactional
    public List<ReportDeliveryLogResponse> deliveryLogs(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return jdbcTemplate.query("""
                select * from report_delivery_logs
                where user_profile_id = ?
                order by attempted_at desc
                limit 40
                """, this::deliveryLogRow, userProfileId);
    }

    @Scheduled(fixedDelayString = "${spendsense.notifications.scheduler-delay-ms:300000}")
    @Transactional
    public void runScheduledEngagementJobs() {
        List<UUID> users = jdbcTemplate.query("select id from user_profiles", (rs, rowNum) -> rs.getObject("id", UUID.class));
        users.forEach(this::generateDeterministicNotifications);
        List<UUID> dueSchedules = jdbcTemplate.query("""
                select id from scheduled_reports
                where active = true and next_run_at <= current_timestamp
                order by next_run_at asc
                limit 100
                """, (rs, rowNum) -> rs.getObject("id", UUID.class));
        dueSchedules.forEach(this::materializeScheduledReport);
    }

    void generateDeterministicNotifications(UUID userProfileId) {
        NotificationPreferenceResponse preferences = ensurePreferences(userProfileId);
        if (!preferences.inAppEnabled()) {
            return;
        }
        if (preferences.recurringRemindersEnabled()) {
            recurringPaymentReminders(userProfileId);
        }
        if (preferences.budgetWarningsEnabled()) {
            budgetWarnings(userProfileId);
        }
        if (preferences.spendingIncreaseEnabled() || preferences.savingsNudgesEnabled()) {
            insightNudges(userProfileId, preferences);
        }
    }

    private void recurringPaymentReminders(UUID userProfileId) {
        LocalDate today = LocalDate.now(clock);
        jdbcTemplate.query("""
                select id, merchant_name, amount, currency, next_expected_on, cadence
                from recurring_transactions
                where user_profile_id = ? and state = 'ACTIVE'
                  and next_expected_on between ? and ?
                order by next_expected_on asc, amount desc
                limit 12
                """, rs -> {
                    LocalDate dueOn = rs.getObject("next_expected_on", LocalDate.class);
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, dueOn);
                    String title = days <= 1 ? "Recurring payment tomorrow" : "Recurring payment coming up";
                    String body = "%s is expected on %s for %s %s.".formatted(
                            rs.getString("merchant_name"),
                            dueOn,
                            rs.getString("currency"),
                            rs.getBigDecimal("amount")
                    );
                    createNotification(
                            userProfileId,
                            "RECURRING_PAYMENT_DUE",
                            "INFO",
                            title,
                            body,
                            "Review payments",
                            "/notifications?tab=recurring",
                            "RECURRING_TRANSACTION",
                            rs.getObject("id", UUID.class).toString(),
                            2,
                            "recurring:%s:%s".formatted(rs.getObject("id", UUID.class), dueOn),
                            Map.of("dueOn", dueOn.toString(), "cadence", rs.getString("cadence"))
                    );
                }, userProfileId, today, today.plusDays(7));
    }

    private void budgetWarnings(UUID userProfileId) {
        LocalDate start = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate endExclusive = start.plusMonths(1);
        jdbcTemplate.query("""
                select b.id, b.name, b.amount, b.currency, c.name as category_name,
                       coalesce(sum(t.amount), 0) as spent
                from budgets b
                join categories c on c.id = b.category_id
                left join transactions t on t.user_profile_id = b.user_profile_id
                    and t.category_id = b.category_id
                    and t.direction = 'DEBIT'
                    and t.status <> 'EXCLUDED'
                    and t.occurred_at >= ?
                    and t.occurred_at < ?
                where b.user_profile_id = ? and b.active = true
                  and b.starts_on <= ? and coalesce(b.ends_on, ?) >= ?
                group by b.id, b.name, b.amount, b.currency, c.name
                having b.amount > 0 and (coalesce(sum(t.amount), 0) / b.amount) >= 0.8
                order by (coalesce(sum(t.amount), 0) / b.amount) desc
                limit 12
                """, rs -> {
                    BigDecimal amount = rs.getBigDecimal("amount");
                    BigDecimal spent = rs.getBigDecimal("spent");
                    int usage = spent.multiply(BigDecimal.valueOf(100)).divide(amount, 0, java.math.RoundingMode.HALF_UP).intValue();
                    boolean exceeded = usage >= 100;
                    createNotification(
                            userProfileId,
                            exceeded ? "BUDGET_EXCEEDED" : "BUDGET_NEARING_LIMIT",
                            exceeded ? "ACTION" : "CAUTION",
                            exceeded ? "Budget fully used" : "Budget nearing limit",
                            "%s is at %d%% of this month's plan.".formatted(rs.getString("category_name"), usage),
                            "Open budget",
                            "/dashboard",
                            "BUDGET",
                            rs.getObject("id", UUID.class).toString(),
                            exceeded ? 1 : 2,
                            "budget:%s:%s:%s".formatted(rs.getObject("id", UUID.class), start, exceeded ? "over" : "near"),
                            Map.of("usagePercent", usage, "spent", spent, "amount", amount)
                    );
                }, start.atStartOfDay().toInstant(ZoneOffset.UTC), endExclusive.atStartOfDay().toInstant(ZoneOffset.UTC), userProfileId, endExclusive.minusDays(1), endExclusive.minusDays(1), start);
    }

    private void insightNudges(UUID userProfileId, NotificationPreferenceResponse preferences) {
        LocalDate today = LocalDate.now(clock);
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, today.withDayOfMonth(1).minusMonths(5), today.withDayOfMonth(1).plusMonths(1).minusDays(1), true);
        if (preferences.spendingIncreaseEnabled()) {
            for (SpendingAnomalyResponse anomaly : insights.anomalies().stream().limit(3).toList()) {
                createNotification(
                        userProfileId,
                        "SPENDING_INCREASE",
                        "CAUTION",
                        "Spending increased",
                        "%s is %s%% above its recent monthly baseline.".formatted(anomaly.categoryName(), anomaly.changePercent()),
                        "View insights",
                        "/insights",
                        "INSIGHT",
                        anomaly.categoryId() == null ? anomaly.categoryName() : anomaly.categoryId().toString(),
                        3,
                        "spend-increase:%s:%s".formatted(anomaly.categoryId() == null ? anomaly.categoryName() : anomaly.categoryId(), YearMonth.from(today)),
                        Map.of("categoryName", anomaly.categoryName(), "changePercent", anomaly.changePercent())
                );
            }
        }
        if (preferences.savingsNudgesEnabled() && insights.savingsTrajectory().size() >= 2) {
            SavingsTrajectoryResponse latest = insights.savingsTrajectory().getLast();
            SavingsTrajectoryResponse previous = insights.savingsTrajectory().get(insights.savingsTrajectory().size() - 2);
            if (latest.netSavings().compareTo(previous.netSavings()) < 0) {
                createNotification(
                        userProfileId,
                        "SAVINGS_DECLINE",
                        "INFO",
                        "Savings momentum softened",
                        "Net savings is lower than the previous reviewed month. A small adjustment can help the trend recover.",
                        "Review report",
                        "/insights",
                        "INSIGHT",
                        "SAVINGS_TRAJECTORY",
                        3,
                        "savings-decline:%s".formatted(YearMonth.from(today)),
                        Map.of("latestNetSavings", latest.netSavings(), "previousNetSavings", previous.netSavings())
                );
            }
        }
    }

    private void materializeScheduledReport(UUID scheduleId) {
        ScheduledReportRow schedule = jdbcTemplate.queryForObject("select * from scheduled_reports where id = ?", this::scheduledReportInternalRow, scheduleId);
        if (schedule == null) {
            return;
        }
        YearMonth reportMonth = YearMonth.from(ZonedDateTime.now(ZoneId.of(schedule.timezone())).minusMonths(schedule.cadence().equals("MONTHLY") ? 1 : 0));
        LocalDate start = schedule.cadence().equals("WEEKLY")
                ? ZonedDateTime.now(ZoneId.of(schedule.timezone())).toLocalDate().minusDays(7)
                : reportMonth.atDay(1);
        LocalDate end = schedule.cadence().equals("WEEKLY")
                ? ZonedDateTime.now(ZoneId.of(schedule.timezone())).toLocalDate().minusDays(1)
                : reportMonth.atEndOfMonth();
        UUID reportId = UUID.randomUUID();
        String filename = "spendsense-%s-%s-%s.%s".formatted(
                schedule.cadence().toLowerCase(),
                schedule.reportType().toLowerCase().replace("_", "-"),
                end,
                schedule.format().toLowerCase()
        );
        jdbcTemplate.update("""
                insert into generated_reports (
                    id, user_profile_id, report_type, format, period_start, period_end, status, file_name,
                    metadata_json, generated_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, 'GENERATED', ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """, reportId, schedule.userProfileId(), schedule.reportType(), schedule.format(), start, end, filename, writeJson(Map.of("scheduledReportId", schedule.id(), "deterministic", true)));
        jdbcTemplate.update("""
                insert into report_delivery_logs (
                    id, scheduled_report_id, generated_report_id, user_profile_id, delivery_channel, status,
                    delivered_at, metadata_json, created_at, updated_at
                ) values (?, ?, ?, ?, ?, 'READY', current_timestamp, ?, current_timestamp, current_timestamp)
                """, UUID.randomUUID(), schedule.id(), reportId, schedule.userProfileId(), schedule.deliveryChannel(), writeJson(Map.of("filename", filename)));
        createNotification(
                schedule.userProfileId(),
                schedule.cadence().equals("WEEKLY") ? "WEEKLY_SUMMARY_READY" : "REPORT_READY",
                "INFO",
                schedule.cadence().equals("WEEKLY") ? "Weekly summary ready" : "Monthly report ready",
                "%s report for %s to %s is ready in %s format.".formatted(schedule.reportType().replace("_", " ").toLowerCase(), start, end, schedule.format()),
                "Manage reports",
                "/notifications?tab=reports",
                "GENERATED_REPORT",
                reportId.toString(),
                2,
                "report-ready:%s".formatted(reportId),
                Map.of("scheduledReportId", schedule.id(), "fileName", filename)
        );
        jdbcTemplate.update("""
                update scheduled_reports
                set last_run_at = current_timestamp, next_run_at = ?, updated_at = current_timestamp
                where id = ?
                """, nextRun(schedule.cadence(), schedule.timezone(), Instant.now(clock).plusSeconds(60)), schedule.id());
    }

    private NotificationPreferenceResponse ensurePreferences(UUID userProfileId) {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from notification_preferences where user_profile_id = ?", Integer.class, userProfileId);
        if (existing == null || existing == 0) {
            jdbcTemplate.update("""
                    insert into notification_preferences (
                        id, user_profile_id, in_app_enabled, budget_warnings_enabled,
                        recurring_reminders_enabled, report_ready_enabled, savings_nudges_enabled,
                        spending_increase_enabled, weekly_digest_enabled, monthly_report_enabled,
                        timezone, created_at, updated_at
                    ) values (?, ?, true, true, true, true, true, true, false, false, ?, current_timestamp, current_timestamp)
                    """, UUID.randomUUID(), userProfileId, "Asia/Kolkata");
        }
        return preference(userProfileId);
    }

    private void createNotification(
            UUID userProfileId,
            String type,
            String severity,
            String title,
            String body,
            String actionLabel,
            String actionUrl,
            String sourceType,
            String sourceId,
            int priority,
            String dedupeKey,
            Object payload
    ) {
        int updated = jdbcTemplate.update("""
                update notifications
                set title = ?, body = ?, severity = ?, priority = ?, payload_json = ?,
                    lifecycle_status = case when lifecycle_status = 'DISMISSED' then 'DISMISSED' else 'ACTIVE' end,
                    updated_at = current_timestamp
                where user_profile_id = ? and dedupe_key = ?
                """,
                title,
                body,
                severity,
                priority,
                writeJson(payload),
                userProfileId,
                dedupeKey
        );
        if (updated > 0) {
            return;
        }
        jdbcTemplate.update("""
                insert into notifications (
                    id, user_profile_id, notification_type, severity, title, body, action_label, action_url,
                    source_type, source_id, delivery_channel, lifecycle_status, priority, dedupe_key,
                    payload_json, delivered_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'IN_APP', 'ACTIVE', ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                UUID.randomUUID(),
                userProfileId,
                type,
                severity,
                title,
                body,
                actionLabel,
                actionUrl,
                sourceType,
                sourceId,
                priority,
                dedupeKey,
                writeJson(payload)
        );
    }

    private List<NotificationResponse> latest(UUID userProfileId, int limit) {
        return jdbcTemplate.query("""
                select * from notifications
                where user_profile_id = ? and lifecycle_status = 'ACTIVE'
                order by created_at desc
                limit ?
                """, this::notificationRow, userProfileId, limit);
    }

    private List<NotificationResponse> timeline(UUID userProfileId, int limit) {
        return jdbcTemplate.query("""
                select * from notifications
                where user_profile_id = ?
                order by created_at desc
                limit ?
                """, this::notificationRow, userProfileId, limit);
    }

    private List<NotificationResponse> notificationsByTypes(UUID userProfileId, List<String> types, int limit) {
        String placeholders = String.join(",", types.stream().map(type -> "?").toList());
        Object[] args = new Object[types.size() + 2];
        args[0] = userProfileId;
        for (int index = 0; index < types.size(); index++) {
            args[index + 1] = types.get(index);
        }
        args[args.length - 1] = limit;
        return jdbcTemplate.query("""
                select * from notifications
                where user_profile_id = ? and lifecycle_status = 'ACTIVE'
                  and notification_type in (%s)
                order by priority asc, created_at desc
                limit ?
                """.formatted(placeholders), this::notificationRow, args);
    }

    private List<ScheduledReportResponse> scheduledReports(UUID userProfileId) {
        return jdbcTemplate.query("""
                select * from scheduled_reports
                where user_profile_id = ?
                order by active desc, next_run_at asc
                """, this::scheduledReportRow, userProfileId);
    }

    private ScheduledReportResponse scheduledReport(UUID id, UUID userProfileId) {
        return jdbcTemplate.queryForObject("select * from scheduled_reports where id = ? and user_profile_id = ?", this::scheduledReportRow, id, userProfileId);
    }

    private NotificationPreferenceResponse preference(UUID userProfileId) {
        return jdbcTemplate.queryForObject("select * from notification_preferences where user_profile_id = ?", this::preferenceRow, userProfileId);
    }

    private long countUnread(UUID userProfileId) {
        Long value = jdbcTemplate.queryForObject("""
                select count(*) from notifications
                where user_profile_id = ? and lifecycle_status = 'ACTIVE' and read_at is null
                """, Long.class, userProfileId);
        return value == null ? 0 : value;
    }

    private long countActive(UUID userProfileId) {
        Long value = jdbcTemplate.queryForObject("""
                select count(*) from notifications
                where user_profile_id = ? and lifecycle_status = 'ACTIVE'
                """, Long.class, userProfileId);
        return value == null ? 0 : value;
    }

    private Instant nextRun(String cadence, String timezone, Instant after) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime cursor = after.atZone(zone);
        ZonedDateTime next = cadence.equals("WEEKLY")
                ? cursor.plusWeeks(1).with(java.time.DayOfWeek.MONDAY).withHour(8).withMinute(30).withSecond(0).withNano(0)
                : cursor.plusMonths(1).withDayOfMonth(1).withHour(8).withMinute(30).withSecond(0).withNano(0);
        if (!next.toInstant().isAfter(after)) {
            next = cadence.equals("WEEKLY") ? next.plusWeeks(1) : next.plusMonths(1);
        }
        return next.toInstant();
    }

    private String normalizeCadence(String cadence) {
        String normalized = cadence == null ? "MONTHLY" : cadence.trim().toUpperCase();
        return normalized.equals("WEEKLY") ? "WEEKLY" : "MONTHLY";
    }

    private String normalizeReportType(String reportType) {
        String normalized = reportType == null ? "MONTHLY_SUMMARY" : reportType.trim().toUpperCase().replace("-", "_");
        return normalized.equals("CATEGORY_REPORT") ? "CATEGORY_REPORT" : "MONTHLY_SUMMARY";
    }

    private String normalizeFormat(String format) {
        String normalized = format == null ? "PDF" : format.trim().toUpperCase();
        return normalized.equals("CSV") ? "CSV" : "PDF";
    }

    private String normalizeChannel(String channel) {
        String normalized = channel == null ? "IN_APP" : channel.trim().toUpperCase();
        return normalized.equals("EMAIL") || normalized.equals("PUSH") ? normalized : "IN_APP";
    }

    private boolean value(Boolean requested, boolean current) {
        return requested == null ? current : requested;
    }

    private NotificationResponse notificationRow(ResultSet rs, int rowNum) throws SQLException {
        Instant readAt = instant(rs, "read_at");
        return new NotificationResponse(
                rs.getObject("id", UUID.class),
                rs.getString("notification_type"),
                rs.getString("severity"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("action_label"),
                rs.getString("action_url"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("delivery_channel"),
                rs.getString("lifecycle_status"),
                rs.getInt("priority"),
                readAt != null,
                instant(rs, "scheduled_for"),
                instant(rs, "delivered_at"),
                readAt,
                instant(rs, "dismissed_at"),
                instant(rs, "expires_at"),
                instant(rs, "created_at")
        );
    }

    private NotificationPreferenceResponse preferenceRow(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationPreferenceResponse(
                rs.getObject("id", UUID.class),
                rs.getBoolean("in_app_enabled"),
                rs.getBoolean("budget_warnings_enabled"),
                rs.getBoolean("recurring_reminders_enabled"),
                rs.getBoolean("report_ready_enabled"),
                rs.getBoolean("savings_nudges_enabled"),
                rs.getBoolean("spending_increase_enabled"),
                rs.getBoolean("weekly_digest_enabled"),
                rs.getBoolean("monthly_report_enabled"),
                rs.getString("timezone"),
                rs.getObject("quiet_hours_start", LocalTime.class),
                rs.getObject("quiet_hours_end", LocalTime.class),
                instant(rs, "updated_at")
        );
    }

    private ScheduledReportResponse scheduledReportRow(ResultSet rs, int rowNum) throws SQLException {
        return new ScheduledReportResponse(
                rs.getObject("id", UUID.class),
                rs.getString("report_type"),
                rs.getString("format"),
                rs.getString("cadence"),
                rs.getString("timezone"),
                rs.getString("delivery_channel"),
                instant(rs, "next_run_at"),
                instant(rs, "last_run_at"),
                rs.getBoolean("active"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private ScheduledReportRow scheduledReportInternalRow(ResultSet rs, int rowNum) throws SQLException {
        return new ScheduledReportRow(
                rs.getObject("id", UUID.class),
                rs.getObject("user_profile_id", UUID.class),
                rs.getString("report_type"),
                rs.getString("format"),
                rs.getString("cadence"),
                rs.getString("timezone"),
                rs.getString("delivery_channel")
        );
    }

    private ReportDeliveryLogResponse deliveryLogRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReportDeliveryLogResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("scheduled_report_id", UUID.class),
                rs.getObject("generated_report_id", UUID.class),
                rs.getString("delivery_channel"),
                rs.getString("status"),
                instant(rs, "attempted_at"),
                instant(rs, "delivered_at"),
                rs.getString("error_message")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write engagement metadata.", exception);
        }
    }

    private record ScheduledReportRow(
            UUID id,
            UUID userProfileId,
            String reportType,
            String format,
            String cadence,
            String timezone,
            String deliveryChannel
    ) {
    }
}
