package com.spendsense.api.service.ai;

import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.dto.ai.AiInsightCardResponse;
import com.spendsense.api.dto.finance.CategoryTrendInsightResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.dto.finance.SpendingAnomalyResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class DeterministicAiProvider implements AiProvider {
    private final SpendSenseProperties properties;

    DeterministicAiProvider(SpendSenseProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiProviderResult generate(AiProviderRequest request) {
        Instant startedAt = Instant.now();
        String content;
        List<AiInsightCardResponse> cards;
        List<String> followUps;
        if (!request.safetyFlags().isEmpty()) {
            content = guardedResponse(request.safetyFlags());
            cards = List.of(new AiInsightCardResponse("SAFETY_BOUNDARY", "CAUTION", "Safe guidance boundary", "SpendSense can explain your own budget, spending, EMI pressure, goals, and reports, but it will not expose hidden instructions, secrets, or investment picks.", BigDecimal.ZERO, BigDecimal.ZERO, "Ask from my data", "GENERAL_FINANCIAL_SUMMARY"));
            followUps = defaultFollowUps();
        } else {
            content = switch (request.intent()) {
                case "OVERSPEND_EXPLANATION", "BUDGET_GUIDANCE" -> overspendResponse(request.context());
                case "EMI_SAFETY" -> emiResponse(request.context());
                case "CATEGORY_SAVINGS_IMPACT" -> categoryImpactResponse(request.context());
                case "HEALTH_SCORE_GUIDANCE" -> healthResponse(request.context());
                case "MONTHLY_CHANGE" -> monthlyChangeResponse(request.context());
                case "GOAL_GUIDANCE" -> goalsResponse(request.context());
                case "RECOMMENDATION_EXPLANATION" -> recommendationResponse(request.context());
                case "HABIT_COACHING" -> habitResponse(request.context());
                case "WEEKLY_RECAP" -> weeklyRecapResponse(request.context());
                default -> summaryResponse(request.context());
            };
            cards = insightCards(request.intent(), request.context());
            followUps = followUps(request.intent());
        }
        int promptTokens = estimateTokens(request.systemPrompt() + " " + request.userPrompt());
        int completionTokens = estimateTokens(content);
        return new AiProviderResult(
                content,
                cards,
                followUps,
                List.of("monthly summaries", "category trends", "budgets", "goals", "recent posted transactions"),
                providerName(),
                modelName(),
                promptTokens,
                completionTokens,
                Math.toIntExact(Math.max(0, Duration.between(startedAt, Instant.now()).toMillis())),
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        );
    }

    private String summaryResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        String cashflowTone = latest.netCashflow().signum() >= 0 ? "income is still ahead of posted spending" : "posted spending is ahead of income";
        String category = topCategory(context).map(CategoryTrendInsightResponse::categoryName).orElse("your largest visible category");
        return """
                Here is the grounded read: this month %s. Income is %s, spending is %s, and net cashflow is %s.

                The clearest place to review is %s because it has the strongest savings impact in the current context. I would treat this as a budgeting conversation, not a prediction: SpendSense is only using posted transactions, active budgets, goals, and deterministic monthly comparisons.

                A calm next step is to review the largest category and any active budget that is above 80 percent usage before adding a new commitment.
                """.formatted(cashflowTone, money(latest.income()), money(latest.expense()), money(latest.netCashflow()), category);
    }

    private String overspendResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        MonthlyComparisonResponse previous = context.previousMonth();
        SpendingAnomalyResponse anomaly = context.anomalies().stream().findFirst().orElse(null);
        AiFinancialContext.BudgetFact budget = context.budgets().stream()
                .filter(item -> item.usagePercent().compareTo(BigDecimal.valueOf(80)) >= 0)
                .max(Comparator.comparing(AiFinancialContext.BudgetFact::usagePercent))
                .orElse(null);
        String reason = anomaly != null
                ? "%s is above its recent baseline by %s.".formatted(anomaly.categoryName(), money(anomaly.absoluteChange()))
                : latest.expense().compareTo(previous.expense()) > 0
                ? "spending rose by %s compared with the previous month.".formatted(money(latest.expenseChange()))
                : "there is no clear overspend spike in the current deterministic summary.";
        String budgetLine = budget == null
                ? "No active budget is currently showing heavy pressure in the compact context."
                : "%s is at %s usage, with %s remaining.".formatted(budget.name(), pct(budget.usagePercent()), money(budget.remaining()));
        return """
                The grounded answer is: %s

                This month spending is %s versus %s last month, and net cashflow is %s. %s

                I would not treat this as a failure signal. It is a traceable pressure point: review the category, check whether any one-off transaction caused the move, and adjust the next budget only after confirming it repeats.
                """.formatted(reason, money(latest.expense()), money(previous.expense()), money(latest.netCashflow()), budgetLine);
    }

    private String emiResponse(AiFinancialContext context) {
        BigDecimal safeByIncome = context.averageIncome().multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safeByCashflow = context.averageFreeCashflow().max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(0.40)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safeLimit = safeByIncome.signum() == 0 ? safeByCashflow : safeByIncome.min(safeByCashflow);
        String state = safeLimit.compareTo(BigDecimal.ZERO) <= 0 ? "tight" : safeLimit.compareTo(context.averageIncome().multiply(BigDecimal.valueOf(0.08))) >= 0 ? "workable" : "thin";
        return """
                Based on the last reviewed months, a cautious EMI limit is around %s per month. That is the smaller of 18 percent of average income (%s) and 40 percent of average free cashflow (%s).

                The current EMI room looks %s because average free cashflow is %s. This is not loan approval advice and it does not assume salary stability, future raises, or investment returns.

                For a precise answer, run the EMI simulator with purchase amount, down payment, rate, tenure, and existing EMIs. SpendSense can then compare the proposed EMI with this deterministic room and show goal delay.
                """.formatted(money(safeLimit), money(safeByIncome), money(safeByCashflow), state, money(context.averageFreeCashflow()));
    }

    private String categoryImpactResponse(AiFinancialContext context) {
        CategoryTrendInsightResponse top = topCategory(context).orElse(null);
        if (top == null) {
            return "I do not have enough category history to name a savings drag yet. Import or categorize more posted transactions, then ask again and I will compare category totals against cashflow.";
        }
        BigDecimal savingsShare = context.latestMonth().netCashflow().signum() == 0
                ? BigDecimal.ZERO
                : top.currentSpend().divide(context.latestMonth().netCashflow().abs(), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return """
                The category hurting savings most is %s, with %s in the current month. Its recent average was %s, so the change is %s.

                That matters because current net cashflow is %s. In plain terms, every small reduction in this category has a direct path back into savings because it is already one of the largest visible outflows.

                The clean next move is to inspect the top merchants inside %s before changing the whole budget. That separates recurring needs from flexible spending.
                """.formatted(top.categoryName(), money(top.currentSpend()), money(top.previousAverage()), pct(top.changePercent()), money(context.latestMonth().netCashflow()), top.categoryName());
    }

    private String healthResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        BigDecimal savingsRate = latest.savingsRate();
        String budgetPressure = context.budgets().stream().anyMatch(budget -> budget.usagePercent().compareTo(BigDecimal.valueOf(100)) >= 0)
                ? "at least one active budget is overspent"
                : context.budgets().stream().anyMatch(budget -> budget.usagePercent().compareTo(BigDecimal.valueOf(80)) >= 0)
                ? "one or more budgets are near their limit"
                : "active budgets look usable from the compact context";
        return """
                To improve the financial health score, focus on the parts SpendSense can measure: savings rate, spending stability, positive cashflow, and budget pressure.

                Right now the latest savings rate is %s and %s. The most direct improvement path is to keep net cashflow positive, reduce the category with the highest current spend, and keep any EMI decision inside the safe cashflow range.

                This is deterministic guidance: no investment returns, no guaranteed outcome, and no assumptions beyond your posted SpendSense data.
                """.formatted(pct(savingsRate), budgetPressure);
    }

    private String monthlyChangeResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        MonthlyComparisonResponse previous = context.previousMonth();
        CategoryTrendInsightResponse top = topCategory(context).orElse(null);
        String categoryLine = top == null
                ? "No category has enough movement to call out confidently."
                : "%s moved to %s against a recent average of %s.".formatted(top.categoryName(), money(top.currentSpend()), money(top.previousAverage()));
        return """
                Compared with last month, income changed by %s and spending changed by %s. Net cashflow is now %s versus %s previously.

                %s

                That is the practical change: SpendSense is not forecasting the next month, just explaining what posted transactions changed in the current summaries.
                """.formatted(money(latest.incomeChange()), money(latest.expenseChange()), money(latest.netCashflow()), money(previous.netCashflow()), categoryLine);
    }

    private String goalsResponse(AiFinancialContext context) {
        AiFinancialContext.GoalFact goal = context.goals().stream().findFirst().orElse(null);
        if (goal == null) {
            return "I do not see an active savings goal in the compact context. Once you add one, I can explain monthly target pressure and how spending changes affect the goal timeline.";
        }
        return """
                Your first visible goal is %s. It has %s saved against a %s target, or %s complete.

                The useful question is whether monthly free cashflow can support steady contributions. Average free cashflow is %s, so goal progress is healthiest when contributions come after essentials, budgets, and existing EMI commitments.

                I would use this as a planning check, not a guarantee: compare the next contribution with current month cashflow before committing it.
                """.formatted(goal.name(), money(goal.currentAmount()), money(goal.targetAmount()), pct(goal.progressPercent()), money(context.averageFreeCashflow()));
    }

    private String recommendationResponse(AiFinancialContext context) {
        CategoryTrendInsightResponse category = topCategory(context).orElse(null);
        AiFinancialContext.BudgetFact budget = context.budgets().stream()
                .filter(item -> item.usagePercent().compareTo(BigDecimal.valueOf(80)) >= 0)
                .findFirst()
                .orElse(null);
        if (budget != null) {
            return """
                    This recommendation is grounded in budget usage. %s is at %s usage, with %s spent against a %s budget.

                    The logic is explainable: SpendSense compares current-month posted debits in that budget category against the active budget amount. If usage is near or above the limit, it suggests a recovery action instead of changing the budget automatically.

                    A calm next step is to inspect the largest merchants in that category and complete the action only when you have actually made the adjustment.
                    """.formatted(budget.name(), pct(budget.usagePercent()), money(budget.spent()), money(budget.amount()));
        }
        if (category != null) {
            return """
                    This recommendation is grounded in category movement. %s is currently %s against a recent average of %s.

                    The logic is deterministic: SpendSense compares posted debit totals by category and suggests a bounded reduction only when the category is large enough to affect cashflow. It does not assume future behavior or make an automatic change.

                    Treat it as a review prompt: confirm whether the spend was necessary, recurring, or flexible before acting.
                    """.formatted(category.categoryName(), money(category.currentSpend()), money(category.previousAverage()));
        }
        return "The current recommendation set is light because there is no strong budget pressure or category spike in the compact context. SpendSense will keep using posted transactions, budgets, goals, and recurring-payment detection only.";
    }

    private String habitResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        long positiveMonths = context.categoryTrends().isEmpty()
                ? 0
                : context.budgets().stream().filter(budget -> budget.usagePercent().compareTo(BigDecimal.valueOf(100)) < 0).count();
        String budgetLine = positiveMonths == 0
                ? "I do not see enough active budget momentum in the compact context."
                : "%d budget area(s) are still below their limit in the compact context.".formatted(positiveMonths);
        return """
                Your habit signal is based on measured behavior, not pressure. Current month net cashflow is %s and savings rate is %s.

                %s Habit coaching should stay practical: protect positive cashflow when it exists, recover one category when it does not, and avoid treating a streak as a moral score.

                SpendSense uses these signals to support awareness, not to create urgency or fake rewards.
                """.formatted(money(latest.netCashflow()), pct(latest.savingsRate()), budgetLine);
    }

    private String weeklyRecapResponse(AiFinancialContext context) {
        MonthlyComparisonResponse latest = context.latestMonth();
        MonthlyComparisonResponse previous = context.previousMonth();
        CategoryTrendInsightResponse category = topCategory(context).orElse(null);
        String categoryLine = category == null
                ? "No category has enough movement to highlight confidently."
                : "%s is the main category to review at %s, compared with %s recently.".formatted(category.categoryName(), money(category.currentSpend()), money(category.previousAverage()));
        return """
                Here is the grounded weekly recap: this month income is %s, spending is %s, and net cashflow is %s.

                Compared with the previous reviewed month, spending changed by %s and income changed by %s. %s

                The useful focus for the week is one action only: either protect the visible surplus or recover the category creating the most pressure. This recap is based on SpendSense summaries only, not predictions.
                """.formatted(money(latest.income()), money(latest.expense()), money(latest.netCashflow()), money(latest.expenseChange()), money(latest.incomeChange()), categoryLine);
    }

    private List<AiInsightCardResponse> insightCards(String intent, AiFinancialContext context) {
        List<AiInsightCardResponse> cards = new ArrayList<>();
        MonthlyComparisonResponse latest = context.latestMonth();
        cards.add(new AiInsightCardResponse("CASHFLOW", latest.netCashflow().signum() >= 0 ? "HEALTHY" : "CAUTION", "Current cashflow", "Income minus posted spending for the current month.", latest.netCashflow(), context.previousMonth().netCashflow(), "Explain change", "MONTHLY_CHANGE"));
        topCategory(context).ifPresent(category -> cards.add(new AiInsightCardResponse("CATEGORY_IMPACT", category.state(), category.categoryName(), "Largest current category pressure in the compact context.", category.currentSpend(), category.previousAverage(), "Explain category", "CATEGORY_SAVINGS_IMPACT")));
        context.budgets().stream()
                .filter(budget -> budget.usagePercent().compareTo(BigDecimal.valueOf(80)) >= 0)
                .findFirst()
                .ifPresent(budget -> cards.add(new AiInsightCardResponse("BUDGET_PRESSURE", budget.state(), budget.name(), "Active budget usage is near or above its limit.", budget.spent(), budget.amount(), "Ask about budget", "BUDGET_GUIDANCE")));
        if (cards.size() == 1 && intent.equals("EMI_SAFETY")) {
            cards.add(new AiInsightCardResponse("EMI_ROOM", "HEALTHY", "EMI room estimate", "Uses income and free cashflow caps before any proposed EMI is entered.", context.averageFreeCashflow().max(BigDecimal.ZERO), context.averageIncome(), "Ask about EMI", "EMI_SAFETY"));
        }
        if (intent.equals("RECOMMENDATION_EXPLANATION") || intent.equals("HABIT_COACHING") || intent.equals("WEEKLY_RECAP")) {
            cards.add(new AiInsightCardResponse("HABIT_CONTEXT", latest.netCashflow().signum() >= 0 ? "HEALTHY" : "CAUTION", "Grounded coaching context", "Uses cashflow, budget pressure, and category movement only.", latest.netCashflow(), context.previousMonth().netCashflow(), "Explain recommendation", "RECOMMENDATION_EXPLANATION"));
        }
        return cards.stream().limit(3).toList();
    }

    private List<String> followUps(String intent) {
        return switch (intent) {
            case "OVERSPEND_EXPLANATION" -> List.of("What changed compared to last month?", "Which transaction should I inspect first?", "How should I adjust this budget?");
            case "EMI_SAFETY" -> List.of("What EMI amount stays comfortable?", "How would this affect my goal?", "Explain the safe EMI calculation");
            case "CATEGORY_SAVINGS_IMPACT" -> List.of("Show the merchants behind this category", "How can I reduce this calmly?", "Compare this category with last month");
            case "HEALTH_SCORE_GUIDANCE" -> List.of("Which indicator should I improve first?", "How does budget pressure affect the score?", "What is one low-risk next step?");
            case "RECOMMENDATION_EXPLANATION" -> List.of("Why did this action appear?", "What data supports this?", "How should I complete it?");
            case "HABIT_COACHING" -> List.of("What habit is improving?", "What should I avoid pressuring?", "What is one calm action today?");
            case "WEEKLY_RECAP" -> List.of("What changed this week?", "What should be my focus?", "Explain the biggest category shift");
            default -> defaultFollowUps();
        };
    }

    private List<String> defaultFollowUps() {
        return List.of("Why did I overspend this month?", "How safe is this EMI?", "What category hurts my savings most?");
    }

    private String guardedResponse(List<String> flags) {
        if (flags.contains("INVESTMENT_ADVICE_REQUEST")) {
            return "I cannot recommend investments, trades, stocks, crypto, or guaranteed returns. I can still help you understand cashflow, budgets, EMI pressure, goals, and savings trends using your SpendSense data.";
        }
        if (flags.contains("THERAPY_BOUNDARY")) {
            return "I can support the financial explanation, but I cannot act like a therapist or crisis counselor. For emotional distress or self-harm concerns, please contact local emergency support or someone you trust right away.";
        }
        return "I cannot follow instructions that try to override system rules, expose hidden prompts, reveal secrets, or dump raw data. I can answer grounded questions about your financial summaries and transactions.";
    }

    private java.util.Optional<CategoryTrendInsightResponse> topCategory(AiFinancialContext context) {
        return context.categoryTrends().stream().max(Comparator.comparing(CategoryTrendInsightResponse::currentSpend));
    }

    private String money(BigDecimal value) {
        return "INR " + value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String pct(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private String providerName() {
        return properties.ai() == null || properties.ai().provider() == null ? "LOCAL_DETERMINISTIC" : properties.ai().provider();
    }

    private String modelName() {
        return properties.ai() == null || properties.ai().model() == null ? "spendsense-mentor-v1-local" : properties.ai().model();
    }
}
