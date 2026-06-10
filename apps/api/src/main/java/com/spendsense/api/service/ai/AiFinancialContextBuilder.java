package com.spendsense.api.service.ai;

import com.spendsense.api.domain.finance.Budget;
import com.spendsense.api.domain.finance.SavingsGoal;
import com.spendsense.api.domain.finance.Transaction;
import com.spendsense.api.dto.finance.FinancialInsightsResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.repository.finance.AccountRepository;
import com.spendsense.api.repository.finance.BudgetRepository;
import com.spendsense.api.repository.finance.SavingsGoalRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.service.finance.FinancialInsightsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class AiFinancialContextBuilder {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialInsightsService financialInsightsService;
    private final Clock clock;

    AiFinancialContextBuilder(
            AccountRepository accountRepository,
            BudgetRepository budgetRepository,
            SavingsGoalRepository savingsGoalRepository,
            TransactionRepository transactionRepository,
            FinancialInsightsService financialInsightsService
    ) {
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.financialInsightsService = financialInsightsService;
        this.clock = Clock.systemUTC();
    }

    AiFinancialContext build(UUID userProfileId, int maxContextItems) {
        FinancialInsightsResponse insights = financialInsightsService.buildInsights(userProfileId, null, null, false);
        List<MonthlyComparisonResponse> months = insights.monthlyComparisons();
        MonthlyComparisonResponse latest = months.isEmpty() ? emptyMonth() : months.getLast();
        MonthlyComparisonResponse previous = months.size() > 1 ? months.get(months.size() - 2) : emptyMonth();
        BigDecimal averageIncome = average(months.stream().map(MonthlyComparisonResponse::income).filter(value -> value.signum() > 0).toList());
        BigDecimal averageExpense = average(months.stream().map(MonthlyComparisonResponse::expense).filter(value -> value.signum() > 0).toList());
        BigDecimal averageFreeCashflow = average(months.stream().map(MonthlyComparisonResponse::netCashflow).toList());
        LocalDate monthStart = LocalDate.now(clock).withDayOfMonth(1);
        Instant from = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = monthStart.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, BigDecimal> currentSpendByCategory = transactionRepository.categorySpendBetween(userProfileId, from, to)
                .stream()
                .filter(row -> row.getCategoryId() != null)
                .collect(Collectors.toMap(row -> UUID.fromString(row.getCategoryId()), TransactionRepository.CategorySpendProjection::getTotal));

        List<AiFinancialContext.BudgetFact> budgets = budgetRepository.findByUserProfileIdAndActiveTrueOrderByStartsOnDescCreatedAtDesc(userProfileId)
                .stream()
                .limit(maxContextItems)
                .map(budget -> budgetFact(currentSpendByCategory, budget))
                .toList();
        List<AiFinancialContext.GoalFact> goals = savingsGoalRepository.findByUserProfileIdOrderByStatusAscTargetDateAscCreatedAtAsc(userProfileId)
                .stream()
                .limit(maxContextItems)
                .map(this::goalFact)
                .toList();
        List<AiFinancialContext.TransactionFact> recentTransactions = transactionRepository.findPostedBetween(userProfileId, monthStart.minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC), to)
                .stream()
                .sorted(Comparator.comparing(Transaction::getOccurredAt).reversed())
                .limit(maxContextItems)
                .map(this::transactionFact)
                .toList();

        return new AiFinancialContext(
                Instant.now(clock),
                accountRepository.sumCurrentBalance(userProfileId).setScale(2, RoundingMode.HALF_UP),
                averageIncome,
                averageExpense,
                averageFreeCashflow,
                latest,
                previous,
                budgets,
                goals,
                recentTransactions,
                insights.anomalies().stream().limit(maxContextItems).toList(),
                insights.categoryTrends().stream().limit(maxContextItems).toList(),
                insights.recurringTransactions().stream().limit(maxContextItems).toList(),
                List.of(
                        "Uses posted transactions only; excluded transactions are not included.",
                        "No market returns, investment recommendations, or guaranteed outcomes are assumed.",
                        "Category and budget guidance is based on current SpendSense summaries."
                )
        );
    }

    private AiFinancialContext.BudgetFact budgetFact(Map<UUID, BigDecimal> currentSpendByCategory, Budget budget) {
        BigDecimal spent = currentSpendByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
        BigDecimal remaining = budget.getAmount().subtract(spent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal usage = percent(spent, budget.getAmount());
        String state = usage.compareTo(BigDecimal.valueOf(100)) >= 0 ? "RISK" : usage.compareTo(BigDecimal.valueOf(80)) >= 0 ? "CAUTION" : "HEALTHY";
        return new AiFinancialContext.BudgetFact(
                budget.getName(),
                budget.getCategory().getName(),
                budget.getAmount(),
                spent,
                remaining,
                usage,
                state
        );
    }

    private AiFinancialContext.GoalFact goalFact(SavingsGoal goal) {
        BigDecimal progress = percent(goal.getCurrentAmount(), goal.getTargetAmount());
        String timelineState = goal.getTargetDate() == null ? "NO_TARGET_DATE" : goal.getTargetDate().isBefore(LocalDate.now(clock)) ? "PAST_TARGET" : "ACTIVE";
        return new AiFinancialContext.GoalFact(goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount(), progress, timelineState);
    }

    private AiFinancialContext.TransactionFact transactionFact(Transaction transaction) {
        return new AiFinancialContext.TransactionFact(
                transaction.getOccurredAt(),
                transaction.getMerchantName(),
                transaction.getCategory() == null ? "Uncategorized" : transaction.getCategory().getName(),
                transaction.getAmount(),
                transaction.getDirection().name()
        );
    }

    private MonthlyComparisonResponse emptyMonth() {
        return new MonthlyComparisonResponse(Instant.now(clock), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
