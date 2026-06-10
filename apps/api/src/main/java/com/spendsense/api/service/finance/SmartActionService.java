package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Budget;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.BehaviorTimelineItem;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.DailySummary;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.FinancialFocus;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.FinancialJourney;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.FinancialMilestone;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.HabitStreakResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.JourneyStep;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.SmartActionResponse;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.SmartReminder;
import com.spendsense.api.dto.engagement.SmartActionDashboardResponse.WeeklyCheckIn;
import com.spendsense.api.dto.engagement.SmartActionStateRequest;
import com.spendsense.api.dto.finance.CategoryTrendInsightResponse;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.dto.finance.RecurringPatternResponse;
import com.spendsense.api.repository.finance.BudgetRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmartActionService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final UserProfileSyncService userProfileSyncService;
    private final FinancialInsightsService financialInsightsService;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SmartActionService(
            UserProfileSyncService userProfileSyncService,
            FinancialInsightsService financialInsightsService,
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.financialInsightsService = financialInsightsService;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public SmartActionDashboardResponse dashboard(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, null, null, false);
        materializeActions(userProfileId, insights);
        List<SmartActionResponse> actions = loadDashboardActions(userProfileId);
        List<HabitStreakResponse> streaks = calculateAndPersistStreaks(userProfileId, insights);
        WeeklyCheckIn weeklyCheckIn = generateWeeklySummary(userProfileId, insights, actions, streaks);
        logEngagement(userProfileId, "SMART_ACTION_DASHBOARD_VIEW", "SMART_ACTION_DASHBOARD", null, Map.of("deterministic", true));
        return new SmartActionDashboardResponse(
                Instant.now(clock),
                dailySummary(insights),
                todayFocus(actions, insights),
                actions,
                streaks,
                weeklyCheckIn,
                milestones(insights, streaks, actions),
                reminders(actions, weeklyCheckIn),
                behaviorTimeline(insights),
                journey(insights, streaks, actions)
        );
    }

    @Transactional
    public SmartActionResponse complete(SupabasePrincipal principal, UUID actionId, SmartActionStateRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return transitionAction(userProfileId, actionId, "COMPLETED", "ACTION_COMPLETED", request == null ? null : request.reason(), null);
    }

    @Transactional
    public SmartActionResponse dismiss(SupabasePrincipal principal, UUID actionId, SmartActionStateRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return transitionAction(userProfileId, actionId, "DISMISSED", "ACTION_DISMISSED", request == null ? null : request.reason(), null);
    }

    @Transactional
    public SmartActionResponse snooze(SupabasePrincipal principal, UUID actionId, SmartActionStateRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        Instant snoozedUntil = request == null || request.snoozedUntil() == null
                ? Instant.now(clock).plusSeconds(3 * 24 * 60 * 60L)
                : request.snoozedUntil();
        return transitionAction(userProfileId, actionId, "SNOOZED", "ACTION_SNOOZED", request == null ? null : request.reason(), snoozedUntil);
    }

    @Transactional
    public WeeklyCheckIn completeWeeklyCheckIn(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        LocalDate weekStart = weekStart();
        jdbcTemplate.update("""
                update weekly_summaries
                set status = 'COMPLETED', completed_at = current_timestamp, updated_at = current_timestamp
                where user_profile_id = ? and week_start = ?
                """, userProfileId, weekStart);
        logEngagement(userProfileId, "WEEKLY_CHECK_IN_COMPLETED", "WEEKLY_SUMMARY", null, Map.of("weekStart", weekStart.toString()));
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, null, null, false);
        return generateWeeklySummary(userProfileId, insights, loadDashboardActions(userProfileId), calculateAndPersistStreaks(userProfileId, insights));
    }

    private void materializeActions(UUID userProfileId, FinancialInsightsResponse insights) {
        for (CandidateAction candidate : candidateActions(userProfileId, insights)) {
            Optional<SmartActionResponse> existing = findByDeterministicKey(userProfileId, candidate.deterministicKey());
            if (existing.isEmpty()) {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update("""
                        insert into smart_actions (
                            id, user_profile_id, deterministic_key, action_type, category, status, priority,
                            title, body, explanation, impact_amount, impact_percent, currency, source_type,
                            source_id, recommendation_json, due_on, generated_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                        """,
                        id,
                        userProfileId,
                        candidate.deterministicKey(),
                        candidate.actionType(),
                        candidate.category(),
                        candidate.priority(),
                        candidate.title(),
                        candidate.body(),
                        candidate.explanation(),
                        candidate.impactAmount(),
                        candidate.impactPercent(),
                        candidate.currency(),
                        candidate.sourceType(),
                        candidate.sourceId(),
                        writeJson(candidate.metadata()),
                        candidate.dueOn()
                );
                insertHistory(userProfileId, id, "GENERATED", null, "OPEN", null, candidate.metadata());
            } else if (existing.get().status().equals("OPEN") || snoozeExpired(existing.get())) {
                jdbcTemplate.update("""
                        update smart_actions
                        set status = 'OPEN', priority = ?, title = ?, body = ?, explanation = ?,
                            impact_amount = ?, impact_percent = ?, currency = ?, recommendation_json = ?,
                            due_on = ?, snoozed_until = null, generated_at = current_timestamp,
                            updated_at = current_timestamp
                        where user_profile_id = ? and deterministic_key = ?
                        """,
                        candidate.priority(),
                        candidate.title(),
                        candidate.body(),
                        candidate.explanation(),
                        candidate.impactAmount(),
                        candidate.impactPercent(),
                        candidate.currency(),
                        writeJson(candidate.metadata()),
                        candidate.dueOn(),
                        userProfileId,
                        candidate.deterministicKey()
                );
            }
        }
    }

    private List<CandidateAction> candidateActions(UUID userProfileId, FinancialInsightsResponse insights) {
        List<CandidateAction> candidates = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
        MonthlyComparisonResponse latest = latestMonth(insights);
        CategoryTrendInsightResponse topCategory = insights.categoryTrends()
                .stream()
                .max(Comparator.comparing(CategoryTrendInsightResponse::currentSpend))
                .orElse(null);

        if (latest.netCashflow().signum() > 0) {
            BigDecimal suggestedSavings = latest.netCashflow().multiply(BigDecimal.valueOf(0.25)).min(BigDecimal.valueOf(5000)).setScale(2, RoundingMode.HALF_UP);
            if (suggestedSavings.compareTo(BigDecimal.valueOf(500)) >= 0) {
                candidates.add(new CandidateAction(
                        "savings-transfer:%s".formatted(currentMonth),
                        "SMART_SAVINGS",
                        "SAVINGS",
                        82,
                        "Move a calm surplus into savings",
                        "Your month-to-date cashflow is positive. Moving %s keeps the action grounded in actual surplus.".formatted(money(suggestedSavings)),
                        "Calculated as 25% of current positive net cashflow, capped at INR 5000. No future income or investment return is assumed.",
                        suggestedSavings,
                        percent(suggestedSavings, latest.income()),
                        "INR",
                        "MONTHLY_CASHFLOW",
                        currentMonth.toString(),
                        LocalDate.now(clock).plusDays(2),
                        Map.of("rule", "positive_cashflow_savings", "netCashflow", latest.netCashflow(), "deterministic", true)
                ));
            }
        } else if (topCategory != null && topCategory.currentSpend().compareTo(BigDecimal.valueOf(1500)) >= 0) {
            BigDecimal recovery = topCategory.currentSpend().multiply(BigDecimal.valueOf(0.12)).min(BigDecimal.valueOf(1200)).setScale(2, RoundingMode.HALF_UP);
            candidates.add(new CandidateAction(
                    "budget-recovery:%s:%s".formatted(currentMonth, topCategory.categoryName().toLowerCase().replaceAll("[^a-z0-9]+", "-")),
                    "BUDGET_RECOVERY",
                    "BUDGET",
                    88,
                    "Recover cashflow from %s".formatted(topCategory.categoryName()),
                    "Reducing %s spend by %s this month would improve net cashflow directly.".formatted(topCategory.categoryName(), money(recovery)),
                    "Calculated as 12% of the current largest spending category, capped at INR 1200, using posted transactions only.",
                    recovery,
                    percent(recovery, topCategory.currentSpend()),
                    "INR",
                    "CATEGORY_TREND",
                    topCategory.categoryId() == null ? topCategory.categoryName() : topCategory.categoryId().toString(),
                    LocalDate.now(clock).plusDays(3),
                    Map.of("rule", "largest_category_recovery", "category", topCategory.categoryName(), "deterministic", true)
            ));
        }

        BigDecimal subscriptionSpend = insights.subscriptions()
                .stream()
                .map(RecurringPatternResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (insights.subscriptions().size() >= 2 && subscriptionSpend.compareTo(BigDecimal.valueOf(500)) >= 0) {
            BigDecimal share = percent(subscriptionSpend, latest.expense());
            candidates.add(new CandidateAction(
                    "subscription-cleanup:%s".formatted(currentMonth),
                    "SUBSCRIPTION_CLEANUP",
                    "SUBSCRIPTIONS",
                    share.compareTo(BigDecimal.valueOf(12)) >= 0 ? 86 : 70,
                    "Review recurring subscriptions",
                    "%d recurring payments total %s and represent %s of current month spending.".formatted(insights.subscriptions().size(), money(subscriptionSpend), pct(share)),
                    "Recurring payments are detected from repeated posted debits with similar amount and cadence. The action asks for review, not automatic cancellation.",
                    subscriptionSpend,
                    share,
                    "INR",
                    "RECURRING_DEBITS",
                    currentMonth.toString(),
                    LocalDate.now(clock).plusDays(5),
                    Map.of("rule", "subscription_share_review", "subscriptionCount", insights.subscriptions().size(), "deterministic", true)
            ));
        }

        candidates.addAll(budgetActions(userProfileId, currentMonth));

        if (insights.savingsTrajectory().size() >= 3) {
            var latestTrajectory = insights.savingsTrajectory().getLast();
            var previousTrajectory = insights.savingsTrajectory().get(insights.savingsTrajectory().size() - 2);
            if (latestTrajectory.netSavings().compareTo(previousTrajectory.netSavings()) < 0) {
                BigDecimal gap = previousTrajectory.netSavings().subtract(latestTrajectory.netSavings()).abs().setScale(2, RoundingMode.HALF_UP);
                candidates.add(new CandidateAction(
                        "savings-consistency:%s".formatted(currentMonth),
                        "SAVINGS_CONSISTENCY",
                        "HABIT",
                        76,
                        "Rebuild savings consistency",
                        "Net savings softened by %s versus the prior month. A smaller recovery target keeps the next step manageable.".formatted(money(gap)),
                        "This compares current and previous monthly net cashflow. The suggestion avoids forecasts and uses the observed gap only.",
                        gap.min(BigDecimal.valueOf(1500)),
                        BigDecimal.ZERO,
                        "INR",
                        "SAVINGS_TRAJECTORY",
                        currentMonth.toString(),
                        LocalDate.now(clock).plusDays(4),
                        Map.of("rule", "savings_trajectory_softened", "gap", gap, "deterministic", true)
                ));
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparing(CandidateAction::priority).reversed())
                .limit(8)
                .toList();
    }

    private List<CandidateAction> budgetActions(UUID userProfileId, YearMonth currentMonth) {
        LocalDate monthStart = currentMonth.atDay(1);
        Instant from = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = currentMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, BigDecimal> spendByCategory = transactionRepository.categorySpendBetween(userProfileId, from, to)
                .stream()
                .filter(row -> row.getCategoryId() != null)
                .collect(Collectors.toMap(row -> UUID.fromString(row.getCategoryId()), TransactionRepository.CategorySpendProjection::getTotal));
        List<CandidateAction> actions = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserProfileIdAndActiveTrueOrderByStartsOnDescCreatedAtDesc(userProfileId)) {
            BigDecimal spent = spendByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
            BigDecimal usage = percent(spent, budget.getAmount());
            if (usage.compareTo(BigDecimal.valueOf(90)) >= 0) {
                BigDecimal recovery = spent.subtract(budget.getAmount().multiply(BigDecimal.valueOf(0.85))).max(BigDecimal.valueOf(250)).setScale(2, RoundingMode.HALF_UP);
                actions.add(new CandidateAction(
                        "budget-pressure:%s:%s".formatted(currentMonth, budget.getId()),
                        "BUDGET_RECOVERY",
                        "BUDGET",
                        usage.compareTo(HUNDRED) >= 0 ? 94 : 84,
                        "Give %s budget a recovery path".formatted(budget.getCategory().getName()),
                        "%s is at %s usage. A %s recovery target would bring the month closer to plan.".formatted(budget.getName(), pct(usage), money(recovery)),
                        "Budget pressure compares current-month posted debit spend against the active budget amount.",
                        recovery,
                        usage,
                        budget.getCurrency(),
                        "BUDGET",
                        budget.getId().toString(),
                        LocalDate.now(clock).plusDays(2),
                        Map.of("rule", "budget_usage_recovery", "budgetId", budget.getId(), "usagePercent", usage, "deterministic", true)
                ));
            }
        }
        return actions;
    }

    private List<HabitStreakResponse> calculateAndPersistStreaks(UUID userProfileId, FinancialInsightsResponse insights) {
        List<StreakCandidate> candidates = new ArrayList<>();
        candidates.add(cashflowStreak(insights));
        candidates.add(dailyRunRateStreak(userProfileId));
        candidates.add(savingsConsistencyStreak(insights));
        List<HabitStreakResponse> responses = new ArrayList<>();
        for (StreakCandidate candidate : candidates) {
            UUID id = upsertStreak(userProfileId, candidate);
            responses.add(new HabitStreakResponse(
                    id,
                    candidate.streakKey(),
                    candidate.label(),
                    candidate.currentCount(),
                    candidate.bestCount(),
                    candidate.unit(),
                    candidate.state(),
                    candidate.lastQualifiedOn(),
                    candidate.explanation()
            ));
        }
        return responses;
    }

    private StreakCandidate cashflowStreak(FinancialInsightsResponse insights) {
        int count = 0;
        List<MonthlyComparisonResponse> months = new ArrayList<>(insights.monthlyComparisons());
        for (int index = months.size() - 1; index >= 0; index--) {
            if (months.get(index).netCashflow().signum() >= 0) {
                count++;
            } else {
                break;
            }
        }
        return new StreakCandidate(
                "positive_cashflow_months",
                "Positive cashflow months",
                count,
                count,
                "months",
                count >= 2 ? "MOMENTUM" : "STEADY",
                LocalDate.now(clock).withDayOfMonth(1),
                count == 0
                        ? "Current month cashflow is still recovering."
                        : "Income has stayed ahead of posted spending for %d month(s).".formatted(count),
                Map.of("rule", "consecutive_non_negative_monthly_cashflow", "deterministic", true)
        );
    }

    private StreakCandidate dailyRunRateStreak(UUID userProfileId) {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        BigDecimal monthlyBudget = budgetRepository.findByUserProfileIdAndActiveTrueOrderByStartsOnDescCreatedAtDesc(userProfileId)
                .stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dailyRunRate = monthlyBudget.signum() == 0
                ? BigDecimal.valueOf(1500)
                : monthlyBudget.divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        Map<LocalDate, BigDecimal> dailySpend = dailyDebits(userProfileId, today.minusDays(13), today.plusDays(1));
        int count = 0;
        for (LocalDate cursor = today; !cursor.isBefore(today.minusDays(13)); cursor = cursor.minusDays(1)) {
            BigDecimal spend = dailySpend.getOrDefault(cursor, BigDecimal.ZERO);
            if (spend.compareTo(dailyRunRate) <= 0) {
                count++;
            } else {
                break;
            }
        }
        return new StreakCandidate(
                "daily_spend_run_rate",
                "Daily spending stayed within run-rate",
                count,
                count,
                "days",
                count >= 4 ? "MOMENTUM" : "STEADY",
                today,
                "%d day(s) in a row stayed at or below the deterministic daily run-rate of %s.".formatted(count, money(dailyRunRate)),
                Map.of("rule", "daily_debits_under_budget_run_rate", "dailyRunRate", dailyRunRate, "deterministic", true)
        );
    }

    private StreakCandidate savingsConsistencyStreak(FinancialInsightsResponse insights) {
        long positiveMonths = insights.savingsTrajectory()
                .stream()
                .filter(point -> point.netSavings().signum() >= 0)
                .count();
        int count = Math.toIntExact(positiveMonths);
        return new StreakCandidate(
                "savings_consistency",
                "Savings consistency",
                count,
                count,
                "months",
                count >= 4 ? "MOMENTUM" : "STEADY",
                LocalDate.now(clock).withDayOfMonth(1),
                "%d reviewed month(s) had non-negative net savings.".formatted(count),
                Map.of("rule", "reviewed_months_with_non_negative_net_savings", "deterministic", true)
        );
    }

    private UUID upsertStreak(UUID userProfileId, StreakCandidate candidate) {
        Optional<UUID> existing = jdbcTemplate.query("""
                select id from financial_streaks where user_profile_id = ? and streak_key = ?
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), userProfileId, candidate.streakKey())
                .stream()
                .findFirst();
        UUID id = existing.orElseGet(UUID::randomUUID);
        if (existing.isEmpty()) {
            jdbcTemplate.update("""
                    insert into financial_streaks (
                        id, user_profile_id, streak_key, label, current_count, best_count, unit, state,
                        last_qualified_on, evaluation_json, calculated_at, created_at, updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                    """,
                    id,
                    userProfileId,
                    candidate.streakKey(),
                    candidate.label(),
                    candidate.currentCount(),
                    candidate.bestCount(),
                    candidate.unit(),
                    candidate.state(),
                    candidate.lastQualifiedOn(),
                    writeJson(candidate.metadata())
            );
        } else {
            jdbcTemplate.update("""
                    update financial_streaks
                    set label = ?, current_count = ?, best_count = greatest(best_count, ?), unit = ?,
                        state = ?, last_qualified_on = ?, evaluation_json = ?, calculated_at = current_timestamp,
                        updated_at = current_timestamp
                    where id = ?
                    """,
                    candidate.label(),
                    candidate.currentCount(),
                    candidate.bestCount(),
                    candidate.unit(),
                    candidate.state(),
                    candidate.lastQualifiedOn(),
                    writeJson(candidate.metadata()),
                    id
            );
        }
        return id;
    }

    private WeeklyCheckIn generateWeeklySummary(
            UUID userProfileId,
            FinancialInsightsResponse insights,
            List<SmartActionResponse> actions,
            List<HabitStreakResponse> streaks
    ) {
        LocalDate weekStart = weekStart();
        LocalDate weekEnd = weekStart.plusDays(6);
        MonthlyComparisonResponse latest = latestMonth(insights);
        List<String> wins = new ArrayList<>();
        streaks.stream().filter(streak -> streak.currentCount() >= 2).findFirst()
                .ifPresent(streak -> wins.add("%s: %d %s".formatted(streak.label(), streak.currentCount(), streak.unit())));
        if (latest.netCashflow().signum() >= 0) {
            wins.add("Income is ahead of posted spending this month.");
        }
        if (wins.isEmpty()) {
            wins.add("You have enough recent data for a grounded check-in.");
        }
        List<String> focus = actions.stream()
                .filter(action -> action.status().equals("OPEN"))
                .limit(3)
                .map(SmartActionResponse::title)
                .toList();
        String headline = latest.netCashflow().signum() >= 0
                ? "This week is about protecting the surplus already visible."
                : "This week is about recovering cashflow with one calm action.";
        Optional<WeeklyCheckIn> existing = loadWeeklySummary(userProfileId, weekStart);
        UUID id = existing.map(WeeklyCheckIn::id).orElseGet(UUID::randomUUID);
        if (existing.isEmpty()) {
            jdbcTemplate.update("""
                    insert into weekly_summaries (
                        id, user_profile_id, week_start, week_end, status, headline, wins_json,
                        focus_json, summary_json, generated_at, created_at, updated_at
                    ) values (?, ?, ?, ?, 'GENERATED', ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                    """,
                    id,
                    userProfileId,
                    weekStart,
                    weekEnd,
                    headline,
                    writeJson(wins),
                    writeJson(focus),
                    writeJson(Map.of("netCashflow", latest.netCashflow(), "deterministic", true))
            );
        } else if (existing.get().status().equals("GENERATED")) {
            jdbcTemplate.update("""
                    update weekly_summaries
                    set headline = ?, wins_json = ?, focus_json = ?, summary_json = ?,
                        generated_at = current_timestamp, updated_at = current_timestamp
                    where id = ?
                    """,
                    headline,
                    writeJson(wins),
                    writeJson(focus),
                    writeJson(Map.of("netCashflow", latest.netCashflow(), "deterministic", true)),
                    id
            );
        }
        return loadWeeklySummary(userProfileId, weekStart).orElseThrow();
    }

    private DailySummary dailySummary(FinancialInsightsResponse insights) {
        MonthlyComparisonResponse latest = latestMonth(insights);
        String tone = latest.netCashflow().signum() >= 0 ? "SUPPORTIVE" : "RECOVERY";
        BigDecimal savingsRate = latest.savingsRate();
        return new DailySummary(
                latest.netCashflow().signum() >= 0 ? "You are protecting positive cashflow." : "A small recovery action is worth choosing today.",
                latest.income(),
                latest.expense(),
                latest.netCashflow(),
                savingsRate,
                tone,
                "Uses current month posted income minus posted debits; excluded transactions are not counted."
        );
    }

    private FinancialFocus todayFocus(List<SmartActionResponse> actions, FinancialInsightsResponse insights) {
        return actions.stream()
                .filter(action -> action.status().equals("OPEN"))
                .findFirst()
                .map(action -> new FinancialFocus(action.title(), action.body(), action.actionType(), action.impactAmount(), action.id().toString()))
                .orElseGet(() -> {
                    MonthlyComparisonResponse latest = latestMonth(insights);
                    return new FinancialFocus(
                            "Keep the ledger calm today",
                            latest.netCashflow().signum() >= 0
                                    ? "No urgent action is open. Keep categorizing new transactions so guidance stays grounded."
                                    : "Review the largest category before changing budgets.",
                            "STEADY_REVIEW",
                            BigDecimal.ZERO,
                            null
                    );
                });
    }

    private List<FinancialMilestone> milestones(
            FinancialInsightsResponse insights,
            List<HabitStreakResponse> streaks,
            List<SmartActionResponse> actions
    ) {
        List<FinancialMilestone> result = new ArrayList<>();
        MonthlyComparisonResponse latest = latestMonth(insights);
        if (latest.netCashflow().signum() >= 0) {
            result.add(new FinancialMilestone("CASHFLOW_WIN", "Cashflow stayed positive", "Income is ahead of current posted spending.", latest.netCashflow(), "HEALTHY"));
        }
        streaks.stream()
                .filter(streak -> streak.currentCount() >= 4)
                .findFirst()
                .ifPresent(streak -> result.add(new FinancialMilestone("STREAK_MOMENTUM", streak.label(), streak.explanation(), BigDecimal.valueOf(streak.currentCount()), "HEALTHY")));
        long completed = actions.stream().filter(action -> action.status().equals("COMPLETED")).count();
        if (completed > 0) {
            result.add(new FinancialMilestone("ACTION_COMPLETION", "Actions completed", "%d recommendation(s) were completed from the action center.".formatted(completed), BigDecimal.valueOf(completed), "HEALTHY"));
        }
        if (result.isEmpty()) {
            result.add(new FinancialMilestone("DATA_READY", "Coaching layer ready", "SpendSense has enough structured data to suggest grounded next steps.", BigDecimal.ZERO, "STEADY"));
        }
        return result;
    }

    private List<SmartReminder> reminders(List<SmartActionResponse> actions, WeeklyCheckIn weeklyCheckIn) {
        List<SmartReminder> result = actions.stream()
                .filter(action -> action.status().equals("OPEN") || action.status().equals("SNOOZED"))
                .limit(4)
                .map(action -> new SmartReminder(
                        action.actionType(),
                        action.status().equals("SNOOZED") ? "Snoozed action" : action.title(),
                        action.status().equals("SNOOZED") ? "This returns when the snooze ends." : action.body(),
                        action.id().toString(),
                        action.snoozedUntil(),
                        action.status()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
        if (!weeklyCheckIn.status().equals("COMPLETED")) {
            result.add(new SmartReminder("WEEKLY_CHECK_IN", "Weekly check-in", weeklyCheckIn.headline(), null, null, "OPEN"));
        }
        return result;
    }

    private List<BehaviorTimelineItem> behaviorTimeline(FinancialInsightsResponse insights) {
        return insights.monthlyComparisons()
                .stream()
                .skip(Math.max(0, insights.monthlyComparisons().size() - 6))
                .map(month -> {
                    String state = month.netCashflow().signum() >= 0 ? "HEALTHY" : "CAUTION";
                    return new BehaviorTimelineItem(
                            YearMonth.from(month.periodStart().atZone(ZoneOffset.UTC).toLocalDate()).toString(),
                            "Net cashflow %s with %s spending.".formatted(money(month.netCashflow()), money(month.expense())),
                            month.periodStart().atZone(ZoneOffset.UTC).toLocalDate(),
                            month.netCashflow(),
                            state
                    );
                })
                .toList();
    }

    private FinancialJourney journey(
            FinancialInsightsResponse insights,
            List<HabitStreakResponse> streaks,
            List<SmartActionResponse> actions
    ) {
        MonthlyComparisonResponse latest = latestMonth(insights);
        int savingsScore = latest.savingsRate().max(BigDecimal.ZERO).min(HUNDRED).setScale(0, RoundingMode.HALF_UP).intValue();
        int streakScore = streaks.stream().mapToInt(streak -> Math.min(100, streak.currentCount() * 12)).max().orElse(0);
        int actionScore = (int) Math.min(100, actions.stream().filter(action -> action.status().equals("COMPLETED")).count() * 25);
        int score = Math.max(0, Math.min(100, Math.round((savingsScore * 0.45f) + (streakScore * 0.35f) + (actionScore * 0.20f))));
        String state = score >= 70 ? "HEALTHY" : score >= 45 ? "STEADY" : "BUILDING";
        return new FinancialJourney(
                score,
                state,
                "Your journey score blends current savings rate, habit momentum, and completed grounded actions.",
                List.of(
                        new JourneyStep("Awareness", "HEALTHY", Math.min(100, insights.monthlyComparisons().size() * 16), "Monthly comparisons are available from imported transactions."),
                        new JourneyStep("Stability", latest.netCashflow().signum() >= 0 ? "HEALTHY" : "BUILDING", savingsScore, "Current month savings rate is measured from posted cashflow."),
                        new JourneyStep("Action", actionScore > 0 ? "HEALTHY" : "STEADY", actionScore, "Completed actions are counted without streak pressure or rewards.")
                )
        );
    }

    private SmartActionResponse transitionAction(UUID userProfileId, UUID actionId, String newStatus, String eventType, String reason, Instant snoozedUntil) {
        SmartActionResponse current = findAction(userProfileId, actionId);
        jdbcTemplate.update("""
                update smart_actions
                set status = ?,
                    completed_at = case when ? = 'COMPLETED' then current_timestamp else completed_at end,
                    dismissed_at = case when ? = 'DISMISSED' then current_timestamp else dismissed_at end,
                    snoozed_until = ?,
                    updated_at = current_timestamp
                where id = ? and user_profile_id = ?
                """,
                newStatus,
                newStatus,
                newStatus,
                snoozedUntil == null ? null : Timestamp.from(snoozedUntil),
                actionId,
                userProfileId
        );
        insertHistory(userProfileId, actionId, eventType, current.status(), newStatus, reason, Map.of("snoozedUntil", snoozedUntil == null ? "" : snoozedUntil.toString()));
        logEngagement(userProfileId, eventType, "SMART_ACTION", actionId, Map.of("previousStatus", current.status(), "newStatus", newStatus));
        return findAction(userProfileId, actionId);
    }

    private SmartActionResponse findAction(UUID userProfileId, UUID actionId) {
        return jdbcTemplate.query("""
                select * from smart_actions where user_profile_id = ? and id = ?
                """, this::actionRow, userProfileId, actionId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new com.spendsense.api.exception.ResourceNotFoundException("Smart action not found."));
    }

    private Optional<SmartActionResponse> findByDeterministicKey(UUID userProfileId, String deterministicKey) {
        return jdbcTemplate.query("""
                select * from smart_actions where user_profile_id = ? and deterministic_key = ?
                """, this::actionRow, userProfileId, deterministicKey)
                .stream()
                .findFirst();
    }

    private List<SmartActionResponse> loadDashboardActions(UUID userProfileId) {
        return jdbcTemplate.query("""
                select * from smart_actions
                where user_profile_id = ?
                  and status in ('OPEN', 'SNOOZED', 'COMPLETED')
                order by case status when 'OPEN' then 0 when 'SNOOZED' then 1 else 2 end,
                         priority desc, generated_at desc
                limit 10
                """, this::actionRow, userProfileId);
    }

    private Optional<WeeklyCheckIn> loadWeeklySummary(UUID userProfileId, LocalDate weekStart) {
        return jdbcTemplate.query("""
                select id, week_start, week_end, status, headline, wins_json, focus_json, generated_at, completed_at
                from weekly_summaries
                where user_profile_id = ? and week_start = ?
                """, this::weeklyRow, userProfileId, weekStart)
                .stream()
                .findFirst();
    }

    private void insertHistory(UUID userProfileId, UUID actionId, String eventType, String previousStatus, String newStatus, String reason, Map<String, ?> metadata) {
        jdbcTemplate.update("""
                insert into action_history (
                    id, user_profile_id, smart_action_id, event_type, previous_status, new_status,
                    reason, metadata_json, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                """,
                UUID.randomUUID(),
                userProfileId,
                actionId,
                eventType,
                previousStatus,
                newStatus,
                reason,
                writeJson(metadata)
        );
    }

    private void logEngagement(UUID userProfileId, String eventType, String sourceType, UUID sourceId, Map<String, ?> metadata) {
        jdbcTemplate.update("""
                insert into engagement_events (
                    id, user_profile_id, event_type, source_type, source_id, metadata_json, occurred_at, created_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                UUID.randomUUID(),
                userProfileId,
                eventType,
                sourceType,
                sourceId,
                writeJson(metadata)
        );
    }

    private Map<LocalDate, BigDecimal> dailyDebits(UUID userProfileId, LocalDate from, LocalDate toExclusive) {
        return jdbcTemplate.query("""
                select cast(t.occurred_at as date) as spend_on, coalesce(sum(t.amount), 0) as total
                from transactions t
                where t.user_profile_id = ?
                  and t.direction = 'DEBIT'
                  and t.status <> 'EXCLUDED'
                  and t.occurred_at >= ?
                  and t.occurred_at < ?
                group by cast(t.occurred_at as date)
                order by spend_on asc
                """,
                (rs, rowNum) -> Map.entry(rs.getObject("spend_on", LocalDate.class), rs.getBigDecimal("total")),
                userProfileId,
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                toExclusive.atStartOfDay().toInstant(ZoneOffset.UTC)
        ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private SmartActionResponse actionRow(ResultSet rs, int rowNum) throws SQLException {
        return new SmartActionResponse(
                rs.getObject("id", UUID.class),
                rs.getString("action_type"),
                rs.getString("category"),
                rs.getString("status"),
                rs.getInt("priority"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("explanation"),
                rs.getBigDecimal("impact_amount"),
                rs.getBigDecimal("impact_percent"),
                rs.getString("currency"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getObject("due_on", LocalDate.class),
                instant(rs, "snoozed_until"),
                instant(rs, "completed_at"),
                instant(rs, "dismissed_at"),
                instant(rs, "generated_at")
        );
    }

    private WeeklyCheckIn weeklyRow(ResultSet rs, int rowNum) throws SQLException {
        return new WeeklyCheckIn(
                rs.getObject("id", UUID.class),
                rs.getObject("week_start", LocalDate.class),
                rs.getObject("week_end", LocalDate.class),
                rs.getString("status"),
                rs.getString("headline"),
                readStringList(rs.getString("wins_json")),
                readStringList(rs.getString("focus_json")),
                instant(rs, "generated_at"),
                instant(rs, "completed_at")
        );
    }

    private boolean snoozeExpired(SmartActionResponse action) {
        return action.status().equals("SNOOZED")
                && action.snoozedUntil() != null
                && action.snoozedUntil().isBefore(Instant.now(clock));
    }

    private MonthlyComparisonResponse latestMonth(FinancialInsightsResponse insights) {
        return insights.monthlyComparisons().isEmpty()
                ? new MonthlyComparisonResponse(Instant.now(clock), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                : insights.monthlyComparisons().getLast();
    }

    private LocalDate weekStart() {
        return LocalDate.now(clock).with(DayOfWeek.MONDAY);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return "INR " + value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String pct(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write smart action metadata.", exception);
        }
    }

    private List<String> readStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private record CandidateAction(
            String deterministicKey,
            String actionType,
            String category,
            int priority,
            String title,
            String body,
            String explanation,
            BigDecimal impactAmount,
            BigDecimal impactPercent,
            String currency,
            String sourceType,
            String sourceId,
            LocalDate dueOn,
            Map<String, ?> metadata
    ) {
    }

    private record StreakCandidate(
            String streakKey,
            String label,
            int currentCount,
            int bestCount,
            String unit,
            String state,
            LocalDate lastQualifiedOn,
            String explanation,
            Map<String, ?> metadata
    ) {
    }
}
