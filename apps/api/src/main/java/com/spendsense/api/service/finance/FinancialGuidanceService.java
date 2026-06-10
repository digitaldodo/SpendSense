package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.SavingsGoal;
import com.spendsense.api.dto.finance.AffordabilityScenarioRequest;
import com.spendsense.api.dto.finance.AffordabilityScenarioResponse;
import com.spendsense.api.dto.finance.CashflowImpactPointResponse;
import com.spendsense.api.dto.finance.FinancialHealthBreakdownResponse;
import com.spendsense.api.dto.finance.FinancialHealthIndicatorResponse;
import com.spendsense.api.dto.finance.FinancialHealthTrendPointResponse;
import com.spendsense.api.dto.finance.ProjectionPointResponse;
import com.spendsense.api.dto.finance.ProjectionRequest;
import com.spendsense.api.dto.finance.ProjectionResponse;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.SavingsGoalRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
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
public class FinancialGuidanceService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext MONEY_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserProfileSyncService userProfileSyncService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public FinancialGuidanceService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            SavingsGoalRepository savingsGoalRepository,
            UserProfileSyncService userProfileSyncService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.userProfileSyncService = userProfileSyncService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public FinancialHealthBreakdownResponse financialHealth(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        List<MonthlyMetrics> months = monthlyMetrics(userProfileId, 6);
        FinancialHealthBreakdownResponse response = buildHealthBreakdown(months);
        persistFinancialSnapshot(userProfileId, response);
        return response;
    }

    @Transactional
    public AffordabilityScenarioResponse simulateAffordability(SupabasePrincipal principal, AffordabilityScenarioRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        List<MonthlyMetrics> months = monthlyMetrics(userProfileId, 6);
        BigDecimal averageIncome = average(months.stream().map(MonthlyMetrics::income).filter(value -> value.signum() > 0).toList());
        BigDecimal averageExpense = average(months.stream().map(MonthlyMetrics::expense).filter(value -> value.signum() > 0).toList());
        BigDecimal averageFreeCashflow = average(months.stream().map(MonthlyMetrics::netCashflow).toList()).max(BigDecimal.ZERO);
        BigDecimal existingEmis = nvl(request.existingMonthlyEmis());
        BigDecimal downPayment = nvl(request.downPayment());
        BigDecimal financedAmount = request.purchaseAmount().subtract(downPayment).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyEmi = emi(financedAmount, request.annualInterestRate(), request.tenureMonths());
        BigDecimal totalPayment = monthlyEmi.multiply(BigDecimal.valueOf(request.tenureMonths())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayment.subtract(financedAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safeEmiLimit = safeEmiLimit(averageIncome, averageFreeCashflow, existingEmis);
        BigDecimal freeAfter = averageFreeCashflow.subtract(monthlyEmi).setScale(2, RoundingMode.HALF_UP);
        BigDecimal reduction = percent(monthlyEmi, averageFreeCashflow);
        BigDecimal savingsImpact = monthlyEmi.multiply(BigDecimal.valueOf(request.tenureMonths())).setScale(2, RoundingMode.HALF_UP);
        Optional<SavingsGoal> goal = selectedGoal(userProfileId, request.goalId());
        Integer delayMonths = goal.map(value -> goalDelayMonths(value, averageFreeCashflow, monthlyEmi)).orElse(null);
        String state = affordabilityState(monthlyEmi, safeEmiLimit, freeAfter, averageIncome, existingEmis);
        String explanation = affordabilityExplanation(state, monthlyEmi, freeAfter, delayMonths, goal.map(SavingsGoal::getName).orElse(null));
        List<CashflowImpactPointResponse> projection = cashflowImpactProjection(averageFreeCashflow, monthlyEmi, request.tenureMonths());
        UUID scenarioId = UUID.randomUUID();
        AffordabilityScenarioResponse response = new AffordabilityScenarioResponse(
                scenarioId,
                Instant.now(clock),
                state,
                explanation,
                request.purchaseAmount().setScale(2, RoundingMode.HALF_UP),
                downPayment.setScale(2, RoundingMode.HALF_UP),
                financedAmount,
                monthlyEmi,
                totalInterest,
                totalPayment,
                safeEmiLimit,
                averageFreeCashflow.setScale(2, RoundingMode.HALF_UP),
                freeAfter,
                reduction,
                savingsImpact,
                delayMonths,
                goal.map(SavingsGoal::getName).orElse(null),
                projection
        );
        persistAffordabilityScenario(userProfileId, request, response);
        return response;
    }

    @Transactional
    public ProjectionResponse project(SupabasePrincipal principal, ProjectionRequest request) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        int monthsToProject = request.months() == null ? 36 : request.months();
        List<MonthlyMetrics> months = monthlyMetrics(userProfileId, 6);
        BigDecimal currentBalance = accountRepository.sumCurrentBalance(userProfileId).setScale(2, RoundingMode.HALF_UP);
        BigDecimal derivedSavings = average(months.stream().map(MonthlyMetrics::netCashflow).toList()).max(BigDecimal.ZERO);
        BigDecimal monthlySavings = request.monthlySavingsOverride() == null ? derivedSavings : request.monthlySavingsOverride();
        BigDecimal derivedExpense = average(months.stream().map(MonthlyMetrics::expense).filter(value -> value.signum() > 0).toList());
        BigDecimal averageExpense = request.emergencyMonthlyExpenseOverride() == null ? derivedExpense : request.emergencyMonthlyExpenseOverride();
        BigDecimal runway = averageExpense.signum() == 0 ? BigDecimal.ZERO : currentBalance.divide(averageExpense, 2, RoundingMode.HALF_UP);
        BigDecimal fireTarget = averageExpense.multiply(BigDecimal.valueOf(12)).multiply(BigDecimal.valueOf(25)).setScale(2, RoundingMode.HALF_UP);
        Integer fireMonths = monthlySavings.signum() == 0 || fireTarget.compareTo(currentBalance) <= 0
                ? null
                : fireTarget.subtract(currentBalance).divide(monthlySavings, 0, RoundingMode.CEILING).intValue();
        List<ProjectionPointResponse> trajectory = projectionTrajectory(currentBalance, monthlySavings, averageExpense, monthsToProject);
        String state = runway.compareTo(BigDecimal.valueOf(6)) >= 0 ? "HEALTHY" : runway.compareTo(BigDecimal.valueOf(3)) >= 0 ? "CAUTION" : "RISK";
        List<String> notes = List.of(
                "Projection uses current balance plus deterministic monthly savings only.",
                "No investment returns, inflation, or market assumptions are included.",
                "Emergency runway divides available balance by average monthly expenses."
        );
        UUID projectionId = UUID.randomUUID();
        ProjectionResponse response = new ProjectionResponse(
                projectionId,
                Instant.now(clock),
                state,
                currentBalance,
                monthlySavings.setScale(2, RoundingMode.HALF_UP),
                averageExpense.setScale(2, RoundingMode.HALF_UP),
                runway,
                fireTarget,
                fireMonths,
                trajectory,
                notes
        );
        persistProjection(userProfileId, request, response);
        return response;
    }

    private FinancialHealthBreakdownResponse buildHealthBreakdown(List<MonthlyMetrics> months) {
        MonthlyMetrics latest = months.getLast();
        MonthlyMetrics previous = months.size() > 1 ? months.get(months.size() - 2) : latest;
        BigDecimal savingsRate = percent(latest.netCashflow(), latest.income());
        BigDecimal spendingStability = stability(months.stream().map(MonthlyMetrics::expense).filter(value -> value.signum() > 0).toList());
        BigDecimal savingsConsistency = savingsConsistency(months);
        BigDecimal debtPressure = BigDecimal.ZERO;
        BigDecimal cashflowQuality = percent(latest.netCashflow().max(BigDecimal.ZERO), latest.income());
        BigDecimal scoreValue = savingsRate.max(BigDecimal.ZERO).min(HUNDRED).multiply(BigDecimal.valueOf(0.28))
                .add(spendingStability.multiply(BigDecimal.valueOf(0.24)))
                .add(savingsConsistency.multiply(BigDecimal.valueOf(0.22)))
                .add(HUNDRED.subtract(debtPressure).multiply(BigDecimal.valueOf(0.10)))
                .add(cashflowQuality.max(BigDecimal.ZERO).min(HUNDRED).multiply(BigDecimal.valueOf(0.16)));
        int score = scoreValue.setScale(0, RoundingMode.HALF_UP).intValue();
        String state = stateForScore(score);
        List<FinancialHealthIndicatorResponse> indicators = List.of(
                indicator("spending_stability", "Spending stability", spendingStability, BigDecimal.valueOf(70), spendingStability.subtract(stabilityValue(List.of(previous.expense(), latest.expense()))), "Recent debit totals are compared with the six-month average.", "Use category budgets for the few areas that move the most."),
                indicator("savings_consistency", "Savings consistency", savingsConsistency, BigDecimal.valueOf(65), latest.netCashflow().subtract(previous.netCashflow()), "Months with positive cashflow improve this indicator.", "Keep goal contributions visible when surplus is actually moved."),
                indicator("debt_pressure", "Debt pressure foundation", HUNDRED.subtract(debtPressure), BigDecimal.valueOf(80), BigDecimal.ZERO, "No linked liability data is assumed; EMI simulations can add pressure explicitly.", "Use the EMI simulator before adding a new monthly commitment."),
                indicator("cashflow_quality", "Cashflow quality", cashflowQuality, BigDecimal.valueOf(20), latest.netCashflow().subtract(previous.netCashflow()), "This checks how much of current income remains after posted debits.", "Prefer decisions that keep monthly free cashflow positive.")
        );
        List<FinancialHealthTrendPointResponse> trend = months.stream()
                .map(month -> {
                    BigDecimal monthSavingsRate = percent(month.netCashflow(), month.income());
                    int monthScore = monthSavingsRate.max(BigDecimal.ZERO).min(HUNDRED)
                            .multiply(BigDecimal.valueOf(0.55))
                            .add(month.netCashflow().signum() >= 0 ? BigDecimal.valueOf(45) : BigDecimal.valueOf(10))
                            .setScale(0, RoundingMode.HALF_UP)
                            .intValue();
                    return new FinancialHealthTrendPointResponse(
                            month.periodStart().atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                            month.income(),
                            month.expense(),
                            month.netCashflow(),
                            monthSavingsRate,
                            Math.max(0, Math.min(100, monthScore)),
                            stateForScore(monthScore)
                    );
                })
                .toList();
        return new FinancialHealthBreakdownResponse(
                Instant.now(clock),
                state,
                Math.max(0, Math.min(100, score)),
                headlineForState(state, latest.netCashflow().subtract(previous.netCashflow())),
                indicators,
                trend
        );
    }

    private List<MonthlyMetrics> monthlyMetrics(UUID userProfileId, int monthCount) {
        YearMonth lastMonth = YearMonth.from(LocalDate.now(clock));
        YearMonth firstMonth = lastMonth.minusMonths(monthCount - 1L);
        Instant from = firstMonth.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = lastMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<YearMonth, TransactionRepository.MonthlySummaryProjection> rows = transactionRepository.monthlySummaryBetween(userProfileId, from, to)
                .stream()
                .collect(Collectors.toMap(row -> YearMonth.from(row.getPeriodStart().toLocalDate()), row -> row));
        List<MonthlyMetrics> result = new ArrayList<>();
        YearMonth cursor = firstMonth;
        while (!cursor.isAfter(lastMonth)) {
            TransactionRepository.MonthlySummaryProjection row = rows.get(cursor);
            BigDecimal income = row == null ? BigDecimal.ZERO : row.getIncome();
            BigDecimal expense = row == null ? BigDecimal.ZERO : row.getExpense();
            result.add(new MonthlyMetrics(cursor, income, expense));
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    private FinancialHealthIndicatorResponse indicator(String key, String label, BigDecimal value, BigDecimal benchmark, BigDecimal change, String explanation, String actionHint) {
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        String state = normalized.compareTo(benchmark) >= 0 ? "HEALTHY" : normalized.compareTo(benchmark.multiply(BigDecimal.valueOf(0.65))) >= 0 ? "CAUTION" : "RISK";
        return new FinancialHealthIndicatorResponse(key, label, state, normalized, benchmark, change.setScale(2, RoundingMode.HALF_UP), explanation, actionHint);
    }

    private BigDecimal emi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (principal.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12_00), 12, RoundingMode.HALF_UP);
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal compound = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths, MONEY_CONTEXT);
        return principal.multiply(monthlyRate).multiply(compound)
                .divide(compound.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeEmiLimit(BigDecimal averageIncome, BigDecimal averageFreeCashflow, BigDecimal existingEmis) {
        BigDecimal incomeLimit = averageIncome.multiply(BigDecimal.valueOf(0.18)).subtract(existingEmis).max(BigDecimal.ZERO);
        BigDecimal cashflowLimit = averageFreeCashflow.multiply(BigDecimal.valueOf(0.40)).max(BigDecimal.ZERO);
        if (incomeLimit.signum() == 0) {
            return cashflowLimit.setScale(2, RoundingMode.HALF_UP);
        }
        return incomeLimit.min(cashflowLimit).setScale(2, RoundingMode.HALF_UP);
    }

    private String affordabilityState(BigDecimal emi, BigDecimal safeLimit, BigDecimal freeAfter, BigDecimal averageIncome, BigDecimal existingEmis) {
        BigDecimal pressure = averageIncome.signum() == 0 ? BigDecimal.ZERO : percent(existingEmis.add(emi), averageIncome);
        if (freeAfter.signum() < 0 || emi.compareTo(safeLimit.multiply(BigDecimal.valueOf(1.25))) > 0 || pressure.compareTo(BigDecimal.valueOf(35)) > 0) {
            return "RISK";
        }
        if (emi.compareTo(safeLimit) > 0 || pressure.compareTo(BigDecimal.valueOf(25)) > 0) {
            return "CAUTION";
        }
        return "HEALTHY";
    }

    private String affordabilityExplanation(String state, BigDecimal emi, BigDecimal freeAfter, Integer delayMonths, String goalName) {
        String base = "This EMI reduces monthly free cashflow by %s and leaves %s.".formatted(emi.toPlainString(), freeAfter.toPlainString());
        if (delayMonths != null && goalName != null && delayMonths > 0) {
            base = base + " It delays %s by about %d month(s).".formatted(goalName, delayMonths);
        }
        if (state.equals("HEALTHY")) {
            return base + " The scenario stays inside the deterministic safe EMI range.";
        }
        if (state.equals("CAUTION")) {
            return base + " The scenario is usable, but it narrows flexibility.";
        }
        return base + " The scenario needs review because monthly flexibility turns thin.";
    }

    private Optional<SavingsGoal> selectedGoal(UUID userProfileId, UUID goalId) {
        if (goalId != null) {
            return savingsGoalRepository.findByIdAndUserProfileId(goalId, userProfileId);
        }
        return savingsGoalRepository.findByUserProfileIdOrderByStatusAscTargetDateAscCreatedAtAsc(userProfileId)
                .stream()
                .filter(goal -> goal.getTargetAmount().compareTo(goal.getCurrentAmount()) > 0)
                .min(Comparator.comparing(goal -> goal.getTargetDate() == null ? LocalDate.MAX : goal.getTargetDate()));
    }

    private int goalDelayMonths(SavingsGoal goal, BigDecimal monthlyFreeCashflow, BigDecimal monthlyEmi) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        if (remaining.signum() == 0 || monthlyFreeCashflow.signum() == 0) {
            return 0;
        }
        BigDecimal afterEmi = monthlyFreeCashflow.subtract(monthlyEmi);
        if (afterEmi.signum() <= 0) {
            return 120;
        }
        int before = remaining.divide(monthlyFreeCashflow, 0, RoundingMode.CEILING).intValue();
        int after = remaining.divide(afterEmi, 0, RoundingMode.CEILING).intValue();
        return Math.max(0, Math.min(120, after - before));
    }

    private List<CashflowImpactPointResponse> cashflowImpactProjection(BigDecimal freeCashflow, BigDecimal emi, int tenureMonths) {
        int visibleMonths = Math.min(24, tenureMonths);
        List<CashflowImpactPointResponse> points = new ArrayList<>();
        BigDecimal simulatedSavings = BigDecimal.ZERO;
        LocalDate cursor = LocalDate.now(clock).withDayOfMonth(1);
        for (int index = 0; index < visibleMonths; index++) {
            BigDecimal simulated = freeCashflow.subtract(emi).setScale(2, RoundingMode.HALF_UP);
            simulatedSavings = simulatedSavings.add(simulated).setScale(2, RoundingMode.HALF_UP);
            points.add(new CashflowImpactPointResponse(cursor.plusMonths(index), freeCashflow.setScale(2, RoundingMode.HALF_UP), simulated, simulatedSavings));
        }
        return points;
    }

    private List<ProjectionPointResponse> projectionTrajectory(BigDecimal currentBalance, BigDecimal monthlySavings, BigDecimal averageExpense, int monthsToProject) {
        List<ProjectionPointResponse> points = new ArrayList<>();
        LocalDate cursor = LocalDate.now(clock).withDayOfMonth(1);
        BigDecimal balance = currentBalance;
        BigDecimal cumulative = BigDecimal.ZERO;
        for (int index = 0; index <= monthsToProject; index++) {
            BigDecimal runway = averageExpense.signum() == 0 ? BigDecimal.ZERO : balance.divide(averageExpense, 2, RoundingMode.HALF_UP);
            points.add(new ProjectionPointResponse(cursor.plusMonths(index), balance.setScale(2, RoundingMode.HALF_UP), cumulative.setScale(2, RoundingMode.HALF_UP), runway));
            balance = balance.add(monthlySavings);
            cumulative = cumulative.add(monthlySavings);
        }
        return points;
    }

    private void persistFinancialSnapshot(UUID userProfileId, FinancialHealthBreakdownResponse response) {
        jdbcTemplate.update("""
                insert into financial_snapshots (
                    id, user_profile_id, snapshot_type, period_start, period_end, state, score,
                    payload_json, generated_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                UUID.randomUUID(),
                userProfileId,
                "HEALTH_BREAKDOWN",
                LocalDate.now(clock).withDayOfMonth(1).minusMonths(5),
                LocalDate.now(clock).withDayOfMonth(1).plusMonths(1).minusDays(1),
                response.state(),
                response.score(),
                writeJson(response)
        );
    }

    private void persistAffordabilityScenario(UUID userProfileId, AffordabilityScenarioRequest request, AffordabilityScenarioResponse response) {
        jdbcTemplate.update("""
                insert into affordability_scenarios (
                    id, user_profile_id, goal_id, purchase_amount, down_payment, financed_amount,
                    annual_interest_rate, tenure_months, monthly_emi, safe_emi_limit,
                    free_cashflow_before, free_cashflow_after, state, payload_json,
                    created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                response.scenarioId(),
                userProfileId,
                request.goalId(),
                response.purchaseAmount(),
                response.downPayment(),
                response.financedAmount(),
                request.annualInterestRate(),
                request.tenureMonths(),
                response.monthlyEmi(),
                response.safeEmiLimit(),
                response.freeCashflowBefore(),
                response.freeCashflowAfter(),
                response.state(),
                writeJson(response)
        );
    }

    private void persistProjection(UUID userProfileId, ProjectionRequest request, ProjectionResponse response) {
        jdbcTemplate.update("""
                insert into projection_history (
                    id, user_profile_id, projection_type, months_projected, starting_balance,
                    monthly_savings, average_monthly_expense, emergency_runway_months,
                    fire_style_target, state, payload_json, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                response.projectionId(),
                userProfileId,
                "FUTURE_BALANCE",
                request.months() == null ? 36 : request.months(),
                response.currentBalance(),
                response.monthlySavings(),
                response.averageMonthlyExpense(),
                response.emergencyRunwayMonths(),
                response.fireStyleTarget(),
                response.state(),
                writeJson(response)
        );
    }

    private BigDecimal savingsConsistency(List<MonthlyMetrics> months) {
        if (months.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long positiveMonths = months.stream().filter(month -> month.netCashflow().signum() >= 0).count();
        return percent(BigDecimal.valueOf(positiveMonths), BigDecimal.valueOf(months.size()));
    }

    private BigDecimal stability(List<BigDecimal> values) {
        return stabilityValue(values);
    }

    private BigDecimal stabilityValue(List<BigDecimal> values) {
        if (values.size() < 2) {
            return HUNDRED;
        }
        BigDecimal avg = average(values);
        if (avg.signum() == 0) {
            return HUNDRED;
        }
        BigDecimal deviation = values.stream()
                .map(value -> value.subtract(avg).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return HUNDRED.subtract(percent(deviation, avg)).max(BigDecimal.ZERO);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String headlineForState(String state, BigDecimal monthlyChange) {
        String direction = monthlyChange.signum() >= 0 ? "improved" : "softened";
        if (state.equals("HEALTHY")) {
            return "Your recent money pattern is steady and cashflow %s this month.".formatted(direction);
        }
        if (state.equals("CAUTION")) {
            return "Your money pattern is workable, with a few areas worth watching calmly.";
        }
        return "Your dashboard has enough signals to review cashflow before adding commitments.";
    }

    private String stateForScore(int score) {
        if (score >= 70) {
            return "HEALTHY";
        }
        if (score >= 45) {
            return "CAUTION";
        }
        return "RISK";
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write financial guidance payload.", exception);
        }
    }

    private record MonthlyMetrics(YearMonth periodStart, BigDecimal income, BigDecimal expense) {
        BigDecimal netCashflow() {
            return income.subtract(expense);
        }
    }
}
