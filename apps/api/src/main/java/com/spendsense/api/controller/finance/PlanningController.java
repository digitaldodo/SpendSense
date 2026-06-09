package com.spendsense.api.controller.finance;

import com.spendsense.api.common.ApiResponse;
import com.spendsense.api.dto.finance.BudgetHistoryResponse;
import com.spendsense.api.dto.finance.BudgetRequest;
import com.spendsense.api.dto.finance.BudgetResponse;
import com.spendsense.api.dto.finance.GoalContributionRequest;
import com.spendsense.api.dto.finance.SavingsGoalRequest;
import com.spendsense.api.dto.finance.SavingsGoalResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.finance.PlanningService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {
    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/budgets")
    ResponseEntity<ApiResponse<List<BudgetResponse>>> listBudgets(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.listBudgets(principal), "Budgets loaded.", traceId));
    }

    @PostMapping("/budgets")
    ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody BudgetRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.createBudget(principal, request), "Budget created.", traceId));
    }

    @PatchMapping("/budgets/{budgetId}")
    ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID budgetId,
            @Valid @RequestBody BudgetRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.updateBudget(principal, budgetId, request), "Budget updated.", traceId));
    }

    @DeleteMapping("/budgets/{budgetId}")
    ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID budgetId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        planningService.deleteBudget(principal, budgetId);
        return ResponseEntity.ok(ApiResponse.success(null, "Budget ended.", traceId));
    }

    @GetMapping("/budgets/history")
    ResponseEntity<ApiResponse<List<BudgetHistoryResponse>>> budgetHistory(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.budgetHistory(principal), "Budget history loaded.", traceId));
    }

    @GetMapping("/goals")
    ResponseEntity<ApiResponse<List<SavingsGoalResponse>>> listGoals(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.listGoals(principal), "Savings goals loaded.", traceId));
    }

    @PostMapping("/goals")
    ResponseEntity<ApiResponse<SavingsGoalResponse>> createGoal(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @Valid @RequestBody SavingsGoalRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.createGoal(principal, request), "Savings goal created.", traceId));
    }

    @PatchMapping("/goals/{goalId}")
    ResponseEntity<ApiResponse<SavingsGoalResponse>> updateGoal(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody SavingsGoalRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.updateGoal(principal, goalId, request), "Savings goal updated.", traceId));
    }

    @DeleteMapping("/goals/{goalId}")
    ResponseEntity<ApiResponse<Void>> deleteGoal(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID goalId,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        planningService.deleteGoal(principal, goalId);
        return ResponseEntity.ok(ApiResponse.success(null, "Savings goal removed.", traceId));
    }

    @PostMapping("/goals/{goalId}/contributions")
    ResponseEntity<ApiResponse<SavingsGoalResponse>> addContribution(
            @AuthenticationPrincipal SupabasePrincipal principal,
            @PathVariable UUID goalId,
            @Valid @RequestBody GoalContributionRequest request,
            @RequestAttribute(name = "traceId", required = false) String traceId
    ) {
        return ResponseEntity.ok(ApiResponse.success(planningService.addGoalContribution(principal, goalId, request), "Contribution added.", traceId));
    }
}
