"use client";

import { authenticatedApiClient } from "@/services/api/authenticated-client";
import type { ApiResponse } from "@/types/api";
import type {
  Account,
  AccountMergePayload,
  BalanceCorrectionPayload,
  BulkTransactionActionPayload,
  BulkTransactionActionResult,
  Category,
  DashboardFinanceSummary,
  DemoSeedResult,
  PageResponse,
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

export async function seedDemoFinanceData() {
  const response = await authenticatedApiClient<ApiResponse<DemoSeedResult>>("/api/v1/demo/finance-seed", {
    method: "POST",
  });
  return response.data;
}
