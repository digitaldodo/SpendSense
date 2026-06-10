"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addGoalContribution,
  bulkUpdateTransactions,
  correctAccountBalance,
  createBudget,
  createCategory,
  createSavingsGoal,
  deleteBudget,
  deleteSavingsGoal,
  getAccounts,
  getBudgetHistory,
  getBudgets,
  getCategories,
  getDashboardFinanceSummary,
  getFinancialInsights,
  getMonthlyReport,
  getSavingsGoals,
  getTransactionDetail,
  getTransactions,
  materializeBudgetRollovers,
  mergeCategory,
  mergeAccount,
  seedDemoFinanceData,
  updateBudget,
  updateCategory,
  updateSavingsGoal,
  updateTransaction,
} from "@/features/finance/services/finance-api";
import type {
  AccountMergePayload,
  BalanceCorrectionPayload,
  BudgetPayload,
  BulkTransactionActionPayload,
  CategoryMergePayload,
  CategoryPayload,
  GoalContributionPayload,
  SavingsGoalPayload,
  TransactionFilters,
  TransactionUpdatePayload,
} from "@/features/finance/types";

export const financeSummaryQueryKey = ["finance", "summary"] as const;
export const accountsQueryKey = ["finance", "accounts"] as const;
export const categoriesQueryKey = ["finance", "categories"] as const;
export const budgetsQueryKey = ["finance", "budgets"] as const;
export const budgetHistoryQueryKey = ["finance", "budgets", "history"] as const;
export const savingsGoalsQueryKey = ["finance", "goals"] as const;
export const insightsQueryKey = ["finance", "insights"] as const;
export const monthlyReportQueryKey = ["finance", "reports", "monthly"] as const;

export function useDashboardFinanceSummary() {
  return useQuery({
    queryKey: financeSummaryQueryKey,
    queryFn: getDashboardFinanceSummary,
    staleTime: 2 * 60_000,
  });
}

export function useAccounts() {
  return useQuery({
    queryKey: accountsQueryKey,
    queryFn: getAccounts,
    staleTime: 5 * 60_000,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: categoriesQueryKey,
    queryFn: getCategories,
    staleTime: 5 * 60_000,
  });
}

export function useBudgets() {
  return useQuery({
    queryKey: budgetsQueryKey,
    queryFn: getBudgets,
  });
}

export function useBudgetHistory() {
  return useQuery({
    queryKey: budgetHistoryQueryKey,
    queryFn: getBudgetHistory,
  });
}

export function useSavingsGoals() {
  return useQuery({
    queryKey: savingsGoalsQueryKey,
    queryFn: getSavingsGoals,
  });
}

export function useFinancialInsights(filters: { from?: string; to?: string } = {}) {
  return useQuery({
    queryKey: [...insightsQueryKey, filters],
    queryFn: () => getFinancialInsights(filters),
  });
}

export function useMonthlyReport(month?: string) {
  return useQuery({
    queryKey: [...monthlyReportQueryKey, month],
    queryFn: () => getMonthlyReport(month),
  });
}

export function useMaterializeBudgetRollovers() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: materializeBudgetRollovers,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useTransactions(filters: TransactionFilters) {
  return useQuery({
    queryKey: ["finance", "transactions", filters],
    queryFn: () => getTransactions(filters),
    placeholderData: keepPreviousData,
  });
}

export function useTransactionDetail(transactionId?: string | null) {
  return useQuery({
    queryKey: ["finance", "transaction-detail", transactionId],
    queryFn: () => getTransactionDetail(transactionId as string),
    enabled: Boolean(transactionId),
  });
}

export function useUpdateTransaction(transactionId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: TransactionUpdatePayload) =>
      updateTransaction(transactionId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useBulkUpdateTransactions() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: BulkTransactionActionPayload) => bulkUpdateTransactions(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useCreateBudget() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: BudgetPayload) => createBudget(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useUpdateBudget(budgetId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: BudgetPayload) => updateBudget(budgetId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useDeleteBudget() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (budgetId: string) => deleteBudget(budgetId),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useCreateSavingsGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: SavingsGoalPayload) => createSavingsGoal(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useUpdateSavingsGoal(goalId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: SavingsGoalPayload) => updateSavingsGoal(goalId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useDeleteSavingsGoal() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (goalId: string) => deleteSavingsGoal(goalId),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useAddGoalContribution(goalId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: GoalContributionPayload) =>
      addGoalContribution(goalId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryPayload) => createCategory(payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useUpdateCategory(categoryId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryPayload) => updateCategory(categoryId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useMergeCategory(categoryId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CategoryMergePayload) => mergeCategory(categoryId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useMergeAccount(accountId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AccountMergePayload) => mergeAccount(accountId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useCorrectAccountBalance(accountId?: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: BalanceCorrectionPayload) =>
      correctAccountBalance(accountId as string, payload),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useSeedDemoFinanceData() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: seedDemoFinanceData,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}
