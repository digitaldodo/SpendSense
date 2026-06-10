"use client";

import { authenticatedApiClient } from "@/services/api/authenticated-client";
import { env } from "@/config/env";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";
import type { ApiResponse } from "@/types/api";
import type {
  Account,
  AccountMergePayload,
  BalanceCorrectionPayload,
  Budget,
  BudgetHistory,
  BudgetPayload,
  BulkTransactionActionPayload,
  BulkTransactionActionResult,
  Category,
  CategoryMergePayload,
  CategoryPayload,
  BudgetRollover,
  DashboardFinanceSummary,
  DemoSeedResult,
  FinancialInsights,
  GeneratedReport,
  GoalContributionPayload,
  PageResponse,
  SavingsGoal,
  SavingsGoalPayload,
  Transaction,
  TransactionDetail,
  TransactionFilters,
  TransactionUpdatePayload,
} from "@/features/finance/types";

function toSearchParams(filters: TransactionFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });
  return params.toString();
}

export async function getAccounts() {
  const response = await authenticatedApiClient<ApiResponse<Account[]>>("/api/v1/accounts");
  return response.data;
}

export async function getCategories() {
  const response = await authenticatedApiClient<ApiResponse<Category[]>>("/api/v1/categories");
  return response.data;
}

export async function createCategory(payload: CategoryPayload) {
  const response = await authenticatedApiClient<ApiResponse<Category>>("/api/v1/categories", {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function updateCategory(categoryId: string, payload: CategoryPayload) {
  const response = await authenticatedApiClient<ApiResponse<Category>>(`/api/v1/categories/${categoryId}`, {
    method: "PATCH",
    body: payload,
  });
  return response.data;
}

export async function mergeCategory(categoryId: string, payload: CategoryMergePayload) {
  const response = await authenticatedApiClient<ApiResponse<Category>>(`/api/v1/categories/${categoryId}/merge`, {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function getBudgets() {
  const response = await authenticatedApiClient<ApiResponse<Budget[]>>("/api/v1/planning/budgets");
  return response.data;
}

export async function createBudget(payload: BudgetPayload) {
  const response = await authenticatedApiClient<ApiResponse<Budget>>("/api/v1/planning/budgets", {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function updateBudget(budgetId: string, payload: BudgetPayload) {
  const response = await authenticatedApiClient<ApiResponse<Budget>>(`/api/v1/planning/budgets/${budgetId}`, {
    method: "PATCH",
    body: payload,
  });
  return response.data;
}

export async function deleteBudget(budgetId: string) {
  const response = await authenticatedApiClient<ApiResponse<null>>(`/api/v1/planning/budgets/${budgetId}`, {
    method: "DELETE",
  });
  return response.data;
}

export async function getBudgetHistory() {
  const response = await authenticatedApiClient<ApiResponse<BudgetHistory[]>>("/api/v1/planning/budgets/history");
  return response.data;
}

export async function getSavingsGoals() {
  const response = await authenticatedApiClient<ApiResponse<SavingsGoal[]>>("/api/v1/planning/goals");
  return response.data;
}

export async function createSavingsGoal(payload: SavingsGoalPayload) {
  const response = await authenticatedApiClient<ApiResponse<SavingsGoal>>("/api/v1/planning/goals", {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function updateSavingsGoal(goalId: string, payload: SavingsGoalPayload) {
  const response = await authenticatedApiClient<ApiResponse<SavingsGoal>>(`/api/v1/planning/goals/${goalId}`, {
    method: "PATCH",
    body: payload,
  });
  return response.data;
}

export async function deleteSavingsGoal(goalId: string) {
  const response = await authenticatedApiClient<ApiResponse<null>>(`/api/v1/planning/goals/${goalId}`, {
    method: "DELETE",
  });
  return response.data;
}

export async function addGoalContribution(goalId: string, payload: GoalContributionPayload) {
  const response = await authenticatedApiClient<ApiResponse<SavingsGoal>>(
    `/api/v1/planning/goals/${goalId}/contributions`,
    {
      method: "POST",
      body: payload,
    }
  );
  return response.data;
}

export async function getTransactions(filters: TransactionFilters = {}) {
  const query = toSearchParams(filters);
  const response = await authenticatedApiClient<ApiResponse<PageResponse<Transaction>>>(
    `/api/v1/transactions${query ? `?${query}` : ""}`
  );
  return response.data;
}

export async function getTransactionDetail(transactionId: string) {
  const response = await authenticatedApiClient<ApiResponse<TransactionDetail>>(
    `/api/v1/transactions/${transactionId}`
  );
  return response.data;
}

export async function updateTransaction(transactionId: string, payload: TransactionUpdatePayload) {
  const response = await authenticatedApiClient<ApiResponse<TransactionDetail>>(
    `/api/v1/transactions/${transactionId}`,
    {
      method: "PATCH",
      body: payload,
    }
  );
  return response.data;
}

export async function bulkUpdateTransactions(payload: BulkTransactionActionPayload) {
  const response = await authenticatedApiClient<ApiResponse<BulkTransactionActionResult>>(
    "/api/v1/transactions/bulk-actions",
    {
      method: "POST",
      body: payload,
    }
  );
  return response.data;
}

export async function mergeAccount(accountId: string, payload: AccountMergePayload) {
  const response = await authenticatedApiClient<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/merge`, {
    method: "POST",
    body: payload,
  });
  return response.data;
}

export async function correctAccountBalance(accountId: string, payload: BalanceCorrectionPayload) {
  const response = await authenticatedApiClient<ApiResponse<Account>>(`/api/v1/accounts/${accountId}/balance`, {
    method: "PATCH",
    body: payload,
  });
  return response.data;
}

export async function getDashboardFinanceSummary() {
  const response = await authenticatedApiClient<ApiResponse<DashboardFinanceSummary>>(
    "/api/v1/transactions/dashboard-summary"
  );
  return response.data;
}

export async function getFinancialInsights(filters: { from?: string; to?: string } = {}) {
  const query = toSearchParams(filters);
  const response = await authenticatedApiClient<ApiResponse<FinancialInsights>>(
    `/api/v1/insights${query ? `?${query}` : ""}`
  );
  return response.data;
}

export async function getMonthlyReport(month?: string) {
  const response = await authenticatedApiClient<ApiResponse<GeneratedReport>>(
    `/api/v1/reports/monthly${month ? `?month=${encodeURIComponent(month)}` : ""}`
  );
  return response.data;
}

export async function materializeBudgetRollovers() {
  const response = await authenticatedApiClient<ApiResponse<BudgetRollover[]>>(
    "/api/v1/insights/rollovers/materialize",
    {
      method: "POST",
    }
  );
  return response.data;
}

export async function downloadReportExport(path: string) {
  const supabase = getSupabaseBrowserClient();
  const {
    data: { session },
  } = await supabase.auth.getSession();
  const headers = new Headers();
  if (session?.access_token) {
    headers.set("Authorization", `Bearer ${session.access_token}`);
  }
  const response = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, { headers });
  if (!response.ok) {
    throw new Error("Report export failed.");
  }
  const blob = await response.blob();
  const disposition = response.headers.get("content-disposition") ?? "";
  const match = disposition.match(/filename="?([^"]+)"?/i);
  return {
    blob,
    filename: match?.[1] ?? "spendsense-report",
  };
}

export async function seedDemoFinanceData() {
  const response = await authenticatedApiClient<ApiResponse<DemoSeedResult>>("/api/v1/demo/finance-seed", {
    method: "POST",
  });
  return response.data;
}
