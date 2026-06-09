package com.spendsense.api.service.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.domain.finance.Budget;
import com.spendsense.api.domain.finance.BudgetHistory;
import com.spendsense.api.domain.finance.Category;
import com.spendsense.api.domain.finance.GoalContribution;
import com.spendsense.api.domain.finance.SavingsGoal;
import com.spendsense.api.dto.finance.BudgetHistoryResponse;
import com.spendsense.api.dto.finance.BudgetOverviewResponse;
import com.spendsense.api.dto.finance.BudgetRequest;
import com.spendsense.api.dto.finance.BudgetResponse;
import com.spendsense.api.dto.finance.CategorySpendResponse;
import com.spendsense.api.dto.finance.CategoryTrendResponse;
import com.spendsense.api.dto.finance.FinancialHealthResponse;
import com.spendsense.api.dto.finance.GoalContributionRequest;
import com.spendsense.api.dto.finance.GoalContributionResponse;
import com.spendsense.api.dto.finance.MonthlySummaryResponse;
import com.spendsense.api.dto.finance.SavingsGoalRequest;
import com.spendsense.api.dto.finance.SavingsGoalResponse;
import com.spendsense.api.dto.finance.SavingsMomentumResponse;
import com.spendsense.api.exception.ResourceNotFoundException;
import com.spendsense.api.mapper.finance.CategoryMapper;
import com.spendsense.api.repository.finance.BudgetHistoryRepository;
import com.spendsense.api.repository.finance.BudgetRepository;
import com.spendsense.api.repository.finance.CategoryRepository;
import com.spendsense.api.repository.finance.GoalContributionRepository;
import com.spendsense.api.repository.finance.SavingsGoalRepository;
import com.spendsense.api.repository.finance.TransactionRepository;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningService {
    private final BudgetRepository budgetRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryMapper categoryMapper;
    private final UserProfileSyncService userProfileSyncService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlanningService(
            BudgetRepository budgetRepository,
            BudgetHistoryRepository budgetHistoryRepository,
            SavingsGoalRepository savingsGoalRepository,
            GoalContributionRepository goalContributionRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            CategoryMapper categoryMapper,
            UserProfileSyncService userProfileSyncService,
            ObjectMapper objectMapper
    ) {
        this.budgetRepository = budgetRepository;
        this.budgetHistoryRepository = budgetHistoryRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.categoryMapper = categoryMapper;
        this.userProfileSyncService = userProfileSyncService;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public List<BudgetResponse> listBudgets(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return buildBudgetResponses(userProfileId, budgetRepository.findByUserProfileIdOrderByActiveDescStartsOnDescCreatedAtDesc(userProfileId));
    }

    @Transactional
    public BudgetResponse createBudget(SupabasePrincipal principal, BudgetRequest request) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Category category = categoryRepository.findVisibleById(request.categoryId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        Budget budget = budgetRepository.save(new Budget(
                profile,
                category,
                request.name().trim(),
                request.amount(),
                request.currency(),
                firstDay(request.startsOn()),
                Boolean.TRUE.equals(request.rolloverEnabled())
        ));
        budgetHistoryRepository.save(history(profile, budget, category, "CREATED", null, budget.getAmount(), null, budget.getName(), null, budget.isActive(), request.reason()));
        return buildBudgetResponses(profile.getId(), List.of(budget)).getFirst();
    }

    @Transactional
    public BudgetResponse updateBudget(SupabasePrincipal principal, UUID budgetId, BudgetRequest request) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Budget budget = budgetRepository.findByIdAndUserProfileId(budgetId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found."));
        Category category = categoryRepository.findVisibleById(request.categoryId(), profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        String previousName = budget.getName();
        BigDecimal previousAmount = budget.getAmount();
        budget.update(category, request.name().trim(), request.amount(), request.currency(), firstDay(request.startsOn()), Boolean.TRUE.equals(request.rolloverEnabled()));
        budgetHistoryRepository.save(history(profile, budget, category, "UPDATED", previousAmount, budget.getAmount(), previousName, budget.getName(), budget.isActive(), budget.isActive(), request.reason()));
        return buildBudgetResponses(profile.getId(), List.of(budgetRepository.save(budget))).getFirst();
    }

    @Transactional
    public void deleteBudget(SupabasePrincipal principal, UUID budgetId) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        Budget budget = budgetRepository.findByIdAndUserProfileId(budgetId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found."));
        boolean wasActive = budget.isActive();
        budget.deactivate(LocalDate.now(clock));
        budgetHistoryRepository.save(history(profile, budget, budget.getCategory(), "DEACTIVATED", budget.getAmount(), budget.getAmount(), budget.getName(), budget.getName(), wasActive, false, "Budget ended by user."));
        budgetRepository.save(budget);
    }

    @Transactional
    public List<BudgetHistoryResponse> budgetHistory(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return budgetHistoryRepository.findTop50ByUserProfileIdOrderByCreatedAtDesc(userProfileId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public List<SavingsGoalResponse> listGoals(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return listGoalResponses(userProfileId);
    }

    @Transactional
    public SavingsGoalResponse createGoal(SupabasePrincipal principal, SavingsGoalRequest request) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        SavingsGoal goal = savingsGoalRepository.save(new SavingsGoal(
                profile,
                request.name().trim(),
                request.targetAmount(),
                request.currentAmount(),
                request.currency(),
                request.targetDate(),
                request.colorToken(),
                request.iconName()
        ));
        if (request.status() != null) {
            goal.update(goal.getName(), goal.getTargetAmount(), goal.getCurrency(), goal.getTargetDate(), request.status(), goal.getColorToken(), goal.getIconName());
        }
        return toGoalResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse updateGoal(SupabasePrincipal principal, UUID goalId, SavingsGoalRequest request) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserProfileId(goalId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));
        goal.update(request.name().trim(), request.targetAmount(), request.currency(), request.targetDate(), request.status(), request.colorToken(), request.iconName());
        return toGoalResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public void deleteGoal(SupabasePrincipal principal, UUID goalId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserProfileId(goalId, userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));
        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoalResponse addGoalContribution(SupabasePrincipal principal, UUID goalId, GoalContributionRequest request) {
        var profile = userProfileSyncService.syncAuthenticatedUser(principal);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserProfileId(goalId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));
        GoalContribution contribution = goalContributionRepository.save(new GoalContribution(
                profile,
                goal,
                request.amount(),
                request.contributedOn() == null ? LocalDate.now(clock) : request.contributedOn(),
                request.note()
        ));
        goal.addContribution(contribution.getAmount());
        return toGoalResponse(savingsGoalRepository.save(goal));
    }

    @Transactional
    public BudgetOverviewResponse budgetOverview(UUID userProfileId) {
        List<BudgetResponse> budgets = buildBudgetResponses(userProfileId, budgetRepository.findByUserProfileIdAndActiveTrueOrderByStartsOnDescCreatedAtDesc(userProfileId));
        BigDecimal totalBudgeted = budgets.stream().map(BudgetResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream().map(BudgetResponse::spent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = totalBudgeted.subtract(totalSpent);
        long overspent = budgets.stream().filter(budget -> budget.remaining().signum() < 0).count();
        return new BudgetOverviewResponse(
                totalBudgeted,
                totalSpent,
                totalRemaining,
                percent(totalSpent, totalBudgeted),
                overspent,
                stateForUsage(percent(totalSpent, totalBudgeted)),
                budgets
        );
    }

    @Transactional
    public List<SavingsGoalResponse> listGoalResponses(UUID userProfileId) {
        return savingsGoalRepository.findByUserProfileIdOrderByStatusAscTargetDateAscCreatedAtAsc(userProfileId)
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Transactional
    public FinancialHealthResponse financialHealth(
            UUID userProfileId,
            BigDecimal monthIncome,
            BigDecimal monthSpend,
            List<MonthlySummaryResponse> monthlySummary,
            BudgetOverviewResponse budgetOverview
    ) {
        BigDecimal savingsRatio = percent(monthIncome.subtract(monthSpend), monthIncome);
        BigDecimal consistency = spendingConsistency(monthlySummary);
        BigDecimal stability = incomeExpenseStability(monthlySummary);
        BigDecimal overspendingFrequency = budgetOverview.budgets().isEmpty()
                ? BigDecimal.ZERO
                : percent(BigDecimal.valueOf(budgetOverview.overspentCount()), BigDecimal.valueOf(budgetOverview.budgets().size()));
        int score = savingsRatio.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(0.35))
                .add(consistency.multiply(BigDecimal.valueOf(0.25)))
                .add(stability.multiply(BigDecimal.valueOf(0.25)))
                .add(BigDecimal.valueOf(100).subtract(overspendingFrequency).multiply(BigDecimal.valueOf(0.15)))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        return new FinancialHealthResponse(Math.max(0, Math.min(100, score)), stateForScore(score), savingsRatio, consistency, stability, overspendingFrequency);
    }

    @Transactional
    public SavingsMomentumResponse savingsMomentum(UUID userProfileId, BigDecimal monthIncome, BigDecimal monthSpend) {
        LocalDate monthStart = LocalDate.now(clock).withDayOfMonth(1);
        BigDecimal contributions = goalContributionRepository.sumContributionsBetween(userProfileId, monthStart, monthStart.plusMonths(1));
        BigDecimal ratio = percent(monthIncome.subtract(monthSpend), monthIncome);
        String state = ratio.compareTo(BigDecimal.valueOf(20)) >= 0 ? "HEALTHY" : ratio.compareTo(BigDecimal.ZERO) >= 0 ? "CAUTION" : "RISK";
        return new SavingsMomentumResponse(monthIncome.subtract(monthSpend), contributions, ratio, state);
    }

    @Transactional
    public List<CategoryTrendResponse> categoryTrends(UUID userProfileId, Instant from, Instant to) {
        return transactionRepository.categoryMonthlySpendBetween(userProfileId, from, to)
                .stream()
                .map(row -> new CategoryTrendResponse(
                        row.getCategoryId() == null ? null : UUID.fromString(row.getCategoryId()),
                        row.getName(),
                        row.getColorToken(),
                        row.getPeriodStart().toInstant(),
                        row.getTotal()
                ))
                .toList();
    }

    public List<CategorySpendResponse> topOverspendingCategories(BudgetOverviewResponse overview) {
        return overview.budgets().stream()
                .filter(budget -> budget.remaining().signum() < 0)
                .sorted(Comparator.comparing(BudgetResponse::remaining))
                .limit(5)
                .map(budget -> new CategorySpendResponse(
                        budget.category().id(),
                        budget.category().name(),
                        budget.category().colorToken(),
                        budget.remaining().abs(),
                        0,
                        budget.usagePercent()
                ))
                .toList();
    }

    private List<BudgetResponse> buildBudgetResponses(UUID userProfileId, List<Budget> budgets) {
        LocalDate monthStart = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        Map<UUID, BigDecimal> spendByCategory = transactionRepository.categorySpendBetween(
                        userProfileId,
                        monthStart.atStartOfDay().toInstant(ZoneOffset.UTC),
                        nextMonthStart.atStartOfDay().toInstant(ZoneOffset.UTC)
                )
                .stream()
                .filter(row -> row.getCategoryId() != null)
                .collect(Collectors.toMap(row -> UUID.fromString(row.getCategoryId()), TransactionRepository.CategorySpendProjection::getTotal));
        return budgets.stream()
                .map(budget -> {
                    BigDecimal spent = spendByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
                    BigDecimal remaining = budget.getAmount().subtract(spent);
                    BigDecimal usage = percent(spent, budget.getAmount());
                    return new BudgetResponse(
                            budget.getId(),
                            budget.getName(),
                            categoryMapper.toResponse(budget.getCategory()),
                            budget.getAmount(),
                            budget.getCurrency(),
                            monthStart,
                            nextMonthStart.minusDays(1),
                            budget.isRolloverEnabled(),
                            budget.isActive(),
                            spent,
                            remaining,
                            usage,
                            stateForUsage(usage)
                    );
                })
                .toList();
    }

    private SavingsGoalResponse toGoalResponse(SavingsGoal goal) {
        List<GoalContributionResponse> contributions = goalContributionRepository.findTop20BySavingsGoalIdOrderByContributedOnDescCreatedAtDesc(goal.getId())
                .stream()
                .map(contribution -> new GoalContributionResponse(
                        contribution.getId(),
                        contribution.getAmount(),
                        contribution.getContributedOn(),
                        contribution.getNote(),
                        contribution.getCreatedAt()
                ))
                .toList();
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getCurrency(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getColorToken(),
                goal.getIconName(),
                percent(goal.getCurrentAmount(), goal.getTargetAmount()),
                remaining,
                monthlyTarget(goal),
                timelineState(goal),
                goal.getCompletedAt(),
                contributions
        );
    }

    private BudgetHistoryResponse toHistoryResponse(BudgetHistory history) {
        return new BudgetHistoryResponse(
                history.getId(),
                history.getBudget() == null ? null : history.getBudget().getId(),
                history.getBudget() == null ? null : history.getBudget().getName(),
                history.getCategory() == null ? null : history.getCategory().getName(),
                history.getAction(),
                history.getPreviousAmount(),
                history.getNewAmount(),
                history.getPreviousName(),
                history.getNewName(),
                history.getPeriodStart(),
                history.getPeriodEnd(),
                history.getReason(),
                history.getCreatedAt()
        );
    }

    private BudgetHistory history(
            com.spendsense.api.domain.user.UserProfile profile,
            Budget budget,
            Category category,
            String action,
            BigDecimal previousAmount,
            BigDecimal newAmount,
            String previousName,
            String newName,
            Boolean previousActive,
            Boolean newActive,
            String reason
    ) {
        return new BudgetHistory(
                profile,
                budget,
                category,
                action,
                previousAmount,
                newAmount,
                previousName,
                newName,
                previousActive,
                newActive,
                LocalDate.now(clock).withDayOfMonth(1),
                LocalDate.now(clock).withDayOfMonth(1).plusMonths(1).minusDays(1),
                writeJson(Map.of(
                        "budgetId", budget.getId(),
                        "categoryId", category.getId(),
                        "active", budget.isActive()
                )),
                reason
        );
    }

    private BigDecimal monthlyTarget(SavingsGoal goal) {
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        if (goal.getTargetDate() == null || remaining.signum() == 0) {
            return BigDecimal.ZERO;
        }
        long months = Math.max(1, ChronoUnit.MONTHS.between(LocalDate.now(clock).withDayOfMonth(1), goal.getTargetDate().withDayOfMonth(1)) + 1);
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private String timelineState(SavingsGoal goal) {
        if (goal.getStatus().name().equals("COMPLETED")) {
            return "COMPLETED";
        }
        if (goal.getTargetDate() == null) {
            return "OPEN";
        }
        LocalDate today = LocalDate.now(clock);
        if (goal.getTargetDate().isBefore(today)) {
            return "PAST_TARGET";
        }
        return monthlyTarget(goal).signum() == 0 ? "ON_TRACK" : "PLANNED";
    }

    private BigDecimal spendingConsistency(List<MonthlySummaryResponse> monthlySummary) {
        List<BigDecimal> expenses = monthlySummary.stream().map(MonthlySummaryResponse::expense).filter(value -> value.signum() > 0).toList();
        if (expenses.size() < 2) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal average = expenses.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
        if (average.signum() == 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal deviation = expenses.stream()
                .map(expense -> expense.subtract(average).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(percent(deviation, average)).max(BigDecimal.ZERO);
    }

    private BigDecimal incomeExpenseStability(List<MonthlySummaryResponse> monthlySummary) {
        if (monthlySummary.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long stableMonths = monthlySummary.stream().filter(month -> month.income().compareTo(month.expense()) >= 0).count();
        return percent(BigDecimal.valueOf(stableMonths), BigDecimal.valueOf(monthlySummary.size()));
    }

    private LocalDate firstDay(LocalDate date) {
        return (date == null ? LocalDate.now(clock) : date).withDayOfMonth(1);
    }

    private String stateForUsage(BigDecimal usage) {
        if (usage.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "RISK";
        }
        if (usage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "CAUTION";
        }
        return "HEALTHY";
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
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write planning audit metadata.", exception);
        }
    }
}
