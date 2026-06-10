package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Budget;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.domain.finance.TransactionDirection;
import com.spendsense.api.dto.finance.BudgetRolloverResponse;
import com.spendsense.api.dto.finance.CategoryDeepDiveResponse;
import com.spendsense.api.dto.finance.CategorySpendResponse;
import com.spendsense.api.dto.finance.CategoryTrendInsightResponse;
import com.spendsense.api.dto.finance.DashboardInsightSummaryResponse;
import com.spendsense.api.dto.finance.DeterministicInsightResponse;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.dto.finance.IncomeStabilityResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.dto.finance.RecurringPatternResponse;
import com.spendsense.api.dto.finance.ReportSummaryResponse;
import com.spendsense.api.dto.finance.SavingsTrajectoryResponse;
import com.spendsense.api.dto.finance.SpendingAnomalyResponse;
import com.spendsense.api.repository.finance.BudgetRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialInsightsService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final UserProfileSyncService userProfileSyncService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public FinancialInsightsService(
            TransactionRepository transactionRepository,
            BudgetRepository budgetRepository,
            UserProfileSyncService userProfileSyncService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.userProfileSyncService = userProfileSyncService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public FinancialInsightsResponse insights(SupabasePrincipal principal, LocalDate from, LocalDate to) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        FinancialInsightsResponse response = buildInsights(userProfileId, from, to, true);
        persistInsightSnapshot(userProfileId, "FINANCIAL_INSIGHTS", response);
        return response;
    }

    @Transactional
    public DashboardInsightSummaryResponse dashboardIndicators(UUID userProfileId) {
        FinancialInsightsResponse response = buildInsights(userProfileId, null, null, false);
        MonthlyComparisonResponse latest = response.monthlyComparisons().isEmpty()
                ? null
                : response.monthlyComparisons().getLast();
        CategoryTrendInsightResponse largestChange = response.categoryTrends().stream()
                .filter(trend -> trend.changePercent().signum() > 0)
                .max(Comparator.comparing(CategoryTrendInsightResponse::changePercent))
                .orElse(null);
        String savingsTrend = response.savingsTrajectory().size() < 2
                ? "STABLE"
                : response.savingsTrajectory().getLast().netSavings().compareTo(response.savingsTrajectory().get(response.savingsTrajectory().size() - 2).netSavings()) >= 0
                ? "IMPROVING"
                : "DECLINING";
        return new DashboardInsightSummaryResponse(
                response.recurringTransactions().size(),
                response.subscriptions().size(),
                response.subscriptions().stream().map(RecurringPatternResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                response.anomalies().size(),
                latest == null ? BigDecimal.ZERO : latest.expenseChangePercent(),
                largestChange == null ? "No material shift" : largestChange.categoryName(),
                response.incomeStability().state(),
                savingsTrend
        );
    }

    @Transactional
    public List<BudgetRolloverResponse> materializeBudgetRollovers(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        LocalDate targetStart = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate targetEnd = targetStart.plusMonths(1).minusDays(1);
        LocalDate sourceStart = targetStart.minusMonths(1);
        LocalDate sourceEnd = targetStart.minusDays(1);
        Instant from = sourceStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = targetStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, BigDecimal> spendByCategory = transactionRepository.categorySpendBetween(userProfileId, from, to)
                .stream()
                .filter(row -> row.getCategoryId() != null)
                .collect(Collectors.toMap(row -> UUID.fromString(row.getCategoryId()), TransactionRepository.CategorySpendProjection::getTotal));
        List<BudgetRolloverResponse> responses = new ArrayList<>();
        for (Budget budget : budgetRepository.findByUserProfileIdAndActiveTrueAndRolloverEnabledTrueOrderByStartsOnDescCreatedAtDesc(userProfileId)) {
            BigDecimal spent = spendByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
            BigDecimal rollover = budget.getAmount().subtract(spent).max(BigDecimal.ZERO);
            responses.add(new BudgetRolloverResponse(
                    budget.getId(),
                    budget.getName(),
                    budget.getCategory().getName(),
                    sourceStart,
                    sourceEnd,
                    targetStart,
                    targetEnd,
                    budget.getAmount(),
                    spent,
                    rollover,
                    "MATERIALIZED"
            ));
            jdbcTemplate.update("""
                    insert into budget_rollovers (
                        id, user_profile_id, budget_id, category_id, source_period_start, source_period_end,
                        target_period_start, target_period_end, original_amount, spent_amount, rollover_amount,
                        state, metadata_json
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (user_profile_id, budget_id, target_period_start)
                    do update set spent_amount = excluded.spent_amount,
                        rollover_amount = excluded.rollover_amount,
                        state = excluded.state,
                        metadata_json = excluded.metadata_json,
                        updated_at = now()
                    """,
                    UUID.randomUUID(),
                    userProfileId,
                    budget.getId(),
                    budget.getCategory().getId(),
                    sourceStart,
                    sourceEnd,
                    targetStart,
                    targetEnd,
                    budget.getAmount(),
                    spent,
                    rollover,
                    "MATERIALIZED",
                    writeJson(Map.of("deterministic", true, "calculation", "max(budget.amount - posted_debits, 0)"))
            );
        }
        return responses;
    }

    FinancialInsightsResponse buildInsights(UUID userProfileId, LocalDate requestedFrom, LocalDate requestedTo, boolean materializeRecurring) {
        LocalDate today = LocalDate.now(clock);
        LocalDate periodStart = requestedFrom == null ? today.withDayOfMonth(1).minusMonths(5) : requestedFrom;
        LocalDate periodEnd = requestedTo == null ? today.withDayOfMonth(1).plusMonths(1).minusDays(1) : requestedTo;
        Instant from = periodStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = periodEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Transaction> transactions = transactionRepository.findPostedBetween(userProfileId, from, toExclusive);
        List<MonthlyComparisonResponse> monthlyComparisons = monthlyComparisons(userProfileId, periodStart, periodEnd);
        List<RecurringPatternResponse> recurring = detectRecurring(transactions);
        if (materializeRecurring) {
            persistRecurringPatterns(userProfileId, recurring);
        }
        List<RecurringPatternResponse> subscriptions = recurring.stream()
                .filter(RecurringPatternResponse::subscription)
                .toList();
        List<SpendingAnomalyResponse> anomalies = spendingAnomalies(userProfileId, periodStart, periodEnd);
        List<CategoryTrendInsightResponse> categoryTrends = categoryTrends(userProfileId, periodStart, periodEnd);
        List<SavingsTrajectoryResponse> savingsTrajectory = savingsTrajectory(monthlyComparisons);
        IncomeStabilityResponse incomeStability = incomeStability(monthlyComparisons);
        ReportSummaryResponse summary = reportSummary(monthlyComparisons, subscriptions, anomalies);
        List<DeterministicInsightResponse> insightCards = insightCards(summary, anomalies, subscriptions, categoryTrends, savingsTrajectory, incomeStability);
        List<CategoryDeepDiveResponse> categoryDeepDives = categoryDeepDives(userProfileId, periodStart, periodEnd);

        return new FinancialInsightsResponse(
                Instant.now(clock),
                from,
                periodEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1),
                "%s to %s".formatted(periodStart, periodEnd),
                summary,
                insightCards,
                anomalies,
                recurring,
                subscriptions,
                monthlyComparisons,
                categoryTrends,
                savingsTrajectory,
                incomeStability,
                categoryDeepDives
        );
    }

    private List<MonthlyComparisonResponse> monthlyComparisons(UUID userProfileId, LocalDate periodStart, LocalDate periodEnd) {
        YearMonth firstMonth = YearMonth.from(periodStart);
        YearMonth lastMonth = YearMonth.from(periodEnd);
        Instant from = firstMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = lastMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<YearMonth, TransactionRepository.MonthlySummaryProjection> rows = transactionRepository.monthlySummaryBetween(userProfileId, from, to)
                .stream()
                .collect(Collectors.toMap(row -> YearMonth.from(row.getPeriodStart().toLocalDate()), row -> row));
        List<MonthlyComparisonResponse> result = new ArrayList<>();
        BigDecimal previousIncome = BigDecimal.ZERO;
        BigDecimal previousExpense = BigDecimal.ZERO;
        YearMonth cursor = firstMonth;
        while (!cursor.isAfter(lastMonth)) {
            TransactionRepository.MonthlySummaryProjection row = rows.get(cursor);
            BigDecimal income = row == null ? BigDecimal.ZERO : row.getIncome();
            BigDecimal expense = row == null ? BigDecimal.ZERO : row.getExpense();
            result.add(new MonthlyComparisonResponse(
                    cursor.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    income,
                    expense,
                    income.subtract(expense),
                    income.subtract(previousIncome),
                    expense.subtract(previousExpense),
                    percent(expense.subtract(previousExpense), previousExpense),
                    percent(income.subtract(expense), income)
            ));
            previousIncome = income;
            previousExpense = expense;
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private List<RecurringPatternResponse> detectRecurring(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getDirection() == TransactionDirection.DEBIT)
                .collect(Collectors.groupingBy(transaction -> transaction.getMerchantNormalized() + "|" + transaction.getCurrency()))
                .values()
                .stream()
                .map(this::toRecurringPattern)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RecurringPatternResponse::confidence).reversed())
                .limit(12)
                .toList();
    }

    private List<RecurringPatternResponse> toRecurringPattern(List<Transaction> group) {
        if (group.size() < 2) {
            return List.of();
        }
        List<Transaction> ordered = group.stream()
                .sorted(Comparator.comparing(Transaction::getOccurredAt))
                .toList();
        BigDecimal averageAmount = ordered.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ordered.size()), 2, RoundingMode.HALF_UP);
        BigDecimal averageDeviation = ordered.stream()
                .map(transaction -> transaction.getAmount().subtract(averageAmount).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ordered.size()), 2, RoundingMode.HALF_UP);
        if (averageDeviation.compareTo(averageAmount.multiply(BigDecimal.valueOf(0.25)).max(BigDecimal.valueOf(75))) > 0) {
            return List.of();
        }
        List<Long> intervals = new ArrayList<>();
        for (int index = 1; index < ordered.size(); index++) {
            intervals.add(ChronoUnit.DAYS.between(
                    ordered.get(index - 1).getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate(),
                    ordered.get(index).getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate()
            ));
        }
        double averageInterval = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        String cadence = cadence(averageInterval);
        if (cadence.equals("IRREGULAR")) {
            return List.of();
        }
        double intervalDeviation = intervals.stream().mapToDouble(value -> Math.abs(value - averageInterval)).average().orElse(0);
        Transaction latest = ordered.getLast();
        int cadenceDays = cadence.equals("WEEKLY") ? 7 : cadence.equals("QUARTERLY") ? 91 : 30;
        BigDecimal confidence = BigDecimal.valueOf(Math.min(95, 54 + ordered.size() * 8 + Math.max(0, 24 - intervalDeviation * 4)))
                .setScale(2, RoundingMode.HALF_UP);
        boolean subscription = averageAmount.signum() > 0
                && confidence.compareTo(BigDecimal.valueOf(70)) >= 0
                && averageDeviation.compareTo(averageAmount.multiply(BigDecimal.valueOf(0.15)).max(BigDecimal.valueOf(40))) <= 0;
        return List.of(new RecurringPatternResponse(
                latest.getCategory() == null ? null : latest.getCategory().getId(),
                latest.getCategory() == null ? "Uncategorized" : latest.getCategory().getName(),
                latest.getMerchantName(),
                latest.getMerchantNormalized(),
                averageAmount,
                latest.getCurrency(),
                cadence,
                ordered.size(),
                ordered.getFirst().getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate(),
                latest.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate(),
                latest.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate().plusDays(cadenceDays),
                confidence,
                subscription
        ));
    }

    private String cadence(double days) {
        if (days >= 6 && days <= 9) {
            return "WEEKLY";
        }
        if (days >= 25 && days <= 35) {
            return "MONTHLY";
        }
        if (days >= 80 && days <= 100) {
            return "QUARTERLY";
        }
        return "IRREGULAR";
    }

    private List<SpendingAnomalyResponse> spendingAnomalies(UUID userProfileId, LocalDate periodStart, LocalDate periodEnd) {
        YearMonth latestMonth = YearMonth.from(periodEnd);
        Instant from = YearMonth.from(periodStart).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = latestMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, List<TransactionRepository.CategoryMonthlySpendProjection>> rowsByCategory = transactionRepository.categoryMonthlySpendBetween(userProfileId, from, to)
                .stream()
                .collect(Collectors.groupingBy(row -> (row.getCategoryId() == null ? "uncategorized" : row.getCategoryId()) + "|" + row.getName()));
        List<SpendingAnomalyResponse> anomalies = new ArrayList<>();
        for (var entry : rowsByCategory.entrySet()) {
            List<TransactionRepository.CategoryMonthlySpendProjection> rows = entry.getValue();
            BigDecimal current = rows.stream()
                    .filter(row -> YearMonth.from(row.getPeriodStart().toLocalDate()).equals(latestMonth))
                    .map(TransactionRepository.CategoryMonthlySpendProjection::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<BigDecimal> previous = rows.stream()
                    .filter(row -> YearMonth.from(row.getPeriodStart().toLocalDate()).isBefore(latestMonth))
                    .map(TransactionRepository.CategoryMonthlySpendProjection::getTotal)
                    .toList();
            if (previous.isEmpty()) {
                continue;
            }
            BigDecimal baseline = previous.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(previous.size()), 2, RoundingMode.HALF_UP);
            BigDecimal change = current.subtract(baseline);
            BigDecimal changePercent = percent(change, baseline);
            if (change.compareTo(BigDecimal.valueOf(500)) >= 0 && changePercent.compareTo(BigDecimal.valueOf(35)) >= 0) {
                TransactionRepository.CategoryMonthlySpendProjection sample = rows.getFirst();
                anomalies.add(new SpendingAnomalyResponse(
                        sample.getCategoryId() == null ? null : UUID.fromString(sample.getCategoryId()),
                        sample.getName(),
                        changePercent.compareTo(BigDecimal.valueOf(75)) >= 0 ? "RISK" : "CAUTION",
                        current,
                        baseline,
                        changePercent,
                        change,
                        "%s is above its recent monthly baseline.".formatted(sample.getName())
                ));
            }
        }
        return anomalies.stream()
                .sorted(Comparator.comparing(SpendingAnomalyResponse::absoluteChange).reversed())
                .limit(8)
                .toList();
    }

    private List<CategoryTrendInsightResponse> categoryTrends(UUID userProfileId, LocalDate periodStart, LocalDate periodEnd) {
        YearMonth latestMonth = YearMonth.from(periodEnd);
        Instant from = YearMonth.from(periodStart).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = latestMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, List<TransactionRepository.CategoryMonthlySpendProjection>> rowsByCategory = transactionRepository.categoryMonthlySpendBetween(userProfileId, from, to)
                .stream()
                .collect(Collectors.groupingBy(row -> (row.getCategoryId() == null ? "uncategorized" : row.getCategoryId()) + "|" + row.getName()));
        return rowsByCategory.values().stream()
                .map(rows -> toCategoryTrend(rows, latestMonth))
                .sorted(Comparator.comparing(CategoryTrendInsightResponse::currentSpend).reversed())
                .limit(10)
                .toList();
    }

    private CategoryTrendInsightResponse toCategoryTrend(List<TransactionRepository.CategoryMonthlySpendProjection> rows, YearMonth latestMonth) {
        TransactionRepository.CategoryMonthlySpendProjection sample = rows.getFirst();
        BigDecimal current = rows.stream()
                .filter(row -> YearMonth.from(row.getPeriodStart().toLocalDate()).equals(latestMonth))
                .map(TransactionRepository.CategoryMonthlySpendProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> previousValues = rows.stream()
                .filter(row -> YearMonth.from(row.getPeriodStart().toLocalDate()).isBefore(latestMonth))
                .map(TransactionRepository.CategoryMonthlySpendProjection::getTotal)
                .toList();
        BigDecimal previousAverage = previousValues.isEmpty()
                ? BigDecimal.ZERO
                : previousValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(previousValues.size()), 2, RoundingMode.HALF_UP);
        BigDecimal changePercent = percent(current.subtract(previousAverage), previousAverage);
        String direction = changePercent.signum() > 0 ? "UP" : changePercent.signum() < 0 ? "DOWN" : "FLAT";
        String state = changePercent.compareTo(BigDecimal.valueOf(40)) >= 0 ? "CAUTION" : "HEALTHY";
        return new CategoryTrendInsightResponse(
                sample.getCategoryId() == null ? null : UUID.fromString(sample.getCategoryId()),
                sample.getName(),
                sample.getColorToken(),
                current,
                previousAverage,
                changePercent,
                direction,
                state
        );
    }

    private List<SavingsTrajectoryResponse> savingsTrajectory(List<MonthlyComparisonResponse> monthlyComparisons) {
        BigDecimal cumulative = BigDecimal.ZERO;
        List<SavingsTrajectoryResponse> trajectory = new ArrayList<>();
        for (MonthlyComparisonResponse month : monthlyComparisons) {
            cumulative = cumulative.add(month.netCashflow());
            trajectory.add(new SavingsTrajectoryResponse(month.periodStart(), month.netCashflow(), month.savingsRate(), cumulative));
        }
        return trajectory;
    }

    private IncomeStabilityResponse incomeStability(List<MonthlyComparisonResponse> monthlyComparisons) {
        List<BigDecimal> incomes = monthlyComparisons.stream()
                .map(MonthlyComparisonResponse::income)
                .filter(value -> value.signum() > 0)
                .toList();
        if (incomes.isEmpty()) {
            return new IncomeStabilityResponse("WAITING", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
        BigDecimal average = incomes.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(incomes.size()), 2, RoundingMode.HALF_UP);
        BigDecimal deviation = incomes.stream()
                .map(income -> income.subtract(average).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(incomes.size()), 2, RoundingMode.HALF_UP);
        BigDecimal score = HUNDRED.subtract(percent(deviation, average)).max(BigDecimal.ZERO);
        String state = score.compareTo(BigDecimal.valueOf(70)) >= 0 ? "HEALTHY" : score.compareTo(BigDecimal.valueOf(45)) >= 0 ? "CAUTION" : "RISK";
        return new IncomeStabilityResponse(state, average, deviation, score, incomes.size());
    }

    private ReportSummaryResponse reportSummary(
            List<MonthlyComparisonResponse> monthlyComparisons,
            List<RecurringPatternResponse> subscriptions,
            List<SpendingAnomalyResponse> anomalies
    ) {
        BigDecimal income = monthlyComparisons.stream().map(MonthlyComparisonResponse::income).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = monthlyComparisons.stream().map(MonthlyComparisonResponse::expense).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportSummaryResponse(
                income,
                expense,
                income.subtract(expense),
                percent(income.subtract(expense), income),
                subscriptions.stream().map(RecurringPatternResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                anomalies.stream().map(SpendingAnomalyResponse::absoluteChange).reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private List<DeterministicInsightResponse> insightCards(
            ReportSummaryResponse summary,
            List<SpendingAnomalyResponse> anomalies,
            List<RecurringPatternResponse> subscriptions,
            List<CategoryTrendInsightResponse> categoryTrends,
            List<SavingsTrajectoryResponse> savingsTrajectory,
            IncomeStabilityResponse incomeStability
    ) {
        List<DeterministicInsightResponse> cards = new ArrayList<>();
        if (!anomalies.isEmpty()) {
            SpendingAnomalyResponse anomaly = anomalies.getFirst();
            cards.add(new DeterministicInsightResponse("SPENDING_SPIKE", anomaly.state(), "Spending spike", anomaly.message(), anomaly.currentSpend(), anomaly.baselineSpend(), "Review category"));
        }
        if (!subscriptions.isEmpty()) {
            cards.add(new DeterministicInsightResponse("SUBSCRIPTIONS", "HEALTHY", "Recurring payments visible", "%d repeating payments were found from posted debits.".formatted(subscriptions.size()), summary.recurringSpend(), BigDecimal.ZERO, "Open subscriptions"));
        }
        categoryTrends.stream()
                .filter(trend -> trend.changePercent().compareTo(BigDecimal.valueOf(35)) >= 0)
                .findFirst()
                .ifPresent(trend -> cards.add(new DeterministicInsightResponse("EXPENSE_TREND", trend.state(), "Category trend rising", "%s is trending above its recent average.".formatted(trend.categoryName()), trend.currentSpend(), trend.previousAverage(), "View category")));
        if (incomeStability.state().equals("RISK") || incomeStability.state().equals("CAUTION")) {
            cards.add(new DeterministicInsightResponse("INCOME_STABILITY", incomeStability.state(), "Income consistency changed", "Recent credits vary more than usual across reviewed months.", incomeStability.stabilityScore(), BigDecimal.valueOf(70), "Compare income"));
        }
        if (savingsTrajectory.size() >= 3) {
            SavingsTrajectoryResponse latest = savingsTrajectory.getLast();
            SavingsTrajectoryResponse previous = savingsTrajectory.get(savingsTrajectory.size() - 2);
            if (latest.netSavings().compareTo(previous.netSavings()) < 0) {
                cards.add(new DeterministicInsightResponse("SAVINGS_DECLINE", "CAUTION", "Savings momentum softened", "Net savings is lower than the previous reviewed month.", latest.netSavings(), previous.netSavings(), "Open report"));
            }
        }
        if (cards.isEmpty()) {
            cards.add(new DeterministicInsightResponse("STEADY_PATTERN", "HEALTHY", "Patterns look steady", "No deterministic spikes or unstable income periods were found for this range.", summary.netCashflow(), BigDecimal.ZERO, "Keep tracking"));
        }
        return cards;
    }

    private List<CategoryDeepDiveResponse> categoryDeepDives(UUID userProfileId, LocalDate periodStart, LocalDate periodEnd) {
        YearMonth latestMonth = YearMonth.from(periodEnd);
        Instant from = YearMonth.from(periodStart).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = latestMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, List<TransactionRepository.CategoryMonthlySpendProjection>> rowsByCategory = transactionRepository.categoryMonthlySpendBetween(userProfileId, from, to)
                .stream()
                .collect(Collectors.groupingBy(row -> (row.getCategoryId() == null ? "uncategorized" : row.getCategoryId()) + "|" + row.getName(), LinkedHashMap::new, Collectors.toList()));
        return rowsByCategory.values().stream()
                .map(rows -> {
                    TransactionRepository.CategoryMonthlySpendProjection sample = rows.getFirst();
                    BigDecimal total = rows.stream().map(TransactionRepository.CategoryMonthlySpendProjection::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = total.divide(BigDecimal.valueOf(Math.max(1, rows.size())), 2, RoundingMode.HALF_UP);
                    CategoryTrendInsightResponse trend = toCategoryTrend(rows, latestMonth);
                    List<MonthlyComparisonResponse> values = rows.stream()
                            .sorted(Comparator.comparing(TransactionRepository.CategoryMonthlySpendProjection::getPeriodStart))
                            .map(row -> new MonthlyComparisonResponse(row.getPeriodStart().toInstant(), BigDecimal.ZERO, row.getTotal(), row.getTotal().negate(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                            .toList();
                    return new CategoryDeepDiveResponse(
                            sample.getCategoryId() == null ? null : UUID.fromString(sample.getCategoryId()),
                            sample.getName(),
                            sample.getColorToken(),
                            total,
                            average,
                            trend.currentSpend(),
                            trend.changePercent(),
                            values
                    );
                })
                .sorted(Comparator.comparing(CategoryDeepDiveResponse::totalSpend).reversed())
                .toList();
    }

    private void persistRecurringPatterns(UUID userProfileId, List<RecurringPatternResponse> recurring) {
        for (RecurringPatternResponse pattern : recurring) {
            jdbcTemplate.update("""
                    insert into recurring_transactions (
                        id, user_profile_id, category_id, merchant_normalized, merchant_name, amount, currency,
                        cadence, occurrence_count, first_seen_on, last_seen_on, next_expected_on, confidence,
                        state, metadata_json
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    on conflict (user_profile_id, merchant_normalized, amount, cadence)
                    do update set category_id = excluded.category_id,
                        merchant_name = excluded.merchant_name,
                        occurrence_count = excluded.occurrence_count,
                        last_seen_on = excluded.last_seen_on,
                        next_expected_on = excluded.next_expected_on,
                        confidence = excluded.confidence,
                        state = excluded.state,
                        metadata_json = excluded.metadata_json,
                        detected_at = now(),
                        updated_at = now()
                    """,
                    UUID.randomUUID(),
                    userProfileId,
                    pattern.categoryId(),
                    pattern.merchantNormalized(),
                    pattern.merchantName(),
                    pattern.amount(),
                    pattern.currency(),
                    pattern.cadence(),
                    pattern.occurrenceCount(),
                    pattern.firstSeenOn(),
                    pattern.lastSeenOn(),
                    pattern.nextExpectedOn(),
                    pattern.confidence(),
                    "ACTIVE",
                    writeJson(Map.of("subscription", pattern.subscription(), "deterministic", true))
            );
        }
    }

    private void persistInsightSnapshot(UUID userProfileId, String type, FinancialInsightsResponse response) {
        jdbcTemplate.update("""
                insert into insight_snapshots (
                    id, user_profile_id, snapshot_type, period_start, period_end, payload_json
                ) values (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                userProfileId,
                type,
                response.periodStart().atZone(ZoneOffset.UTC).toLocalDate(),
                response.periodEnd().atZone(ZoneOffset.UTC).toLocalDate(),
                writeJson(response)
        );
    }

    List<CategorySpendResponse> categoryBreakdown(UUID userProfileId, LocalDate periodStart, LocalDate periodEnd) {
        Instant from = periodStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = periodEnd.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var rows = transactionRepository.categorySpendBetween(userProfileId, from, to);
        BigDecimal total = rows.stream().map(TransactionRepository.CategorySpendProjection::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream()
                .map(row -> new CategorySpendResponse(
                        row.getCategoryId() == null ? null : UUID.fromString(row.getCategoryId()),
                        row.getName(),
                        row.getColorToken(),
                        row.getTotal(),
                        row.getTransactionCount() == null ? 0 : row.getTransactionCount(),
                        percent(row.getTotal(), total)
                ))
                .toList();
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write deterministic insight metadata.", exception);
        }
    }
}
