"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  bulkUpdateTransactions,
  correctAccountBalance,
  getAccounts,
  getCategories,
  getDashboardFinanceSummary,
  getTransactionDetail,
  getTransactions,
  mergeAccount,
  seedDemoFinanceData,
  updateTransaction,
} from "@/features/finance/services/finance-api";
import type {
  AccountMergePayload,
  BalanceCorrectionPayload,
  BulkTransactionActionPayload,
  TransactionFilters,
  TransactionUpdatePayload,
} from "@/features/finance/types";

export const financeSummaryQueryKey = ["finance", "summary"] as const;
export const accountsQueryKey = ["finance", "accounts"] as const;
export const categoriesQueryKey = ["finance", "categories"] as const;

export function useDashboardFinanceSummary() {
  return useQuery({
    queryKey: financeSummaryQueryKey,
    queryFn: getDashboardFinanceSummary,
  });
}

export function useAccounts() {
  return useQuery({
    queryKey: accountsQueryKey,
    queryFn: getAccounts,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: categoriesQueryKey,
    queryFn: getCategories,
  });
}

export function useTransactions(filters: TransactionFilters) {
  return useQuery({
    queryKey: ["finance", "transactions", filters],
    queryFn: () => getTransactions(filters),
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
    mutationFn: (payload: TransactionUpdatePayload) => updateTransaction(transactionId as string, payload),
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
    mutationFn: (payload: BalanceCorrectionPayload) => correctAccountBalance(accountId as string, payload),
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
