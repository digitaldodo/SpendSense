"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAccounts,
  getCategories,
  getDashboardFinanceSummary,
  getTransactionDetail,
  getTransactions,
  seedDemoFinanceData,
} from "@/features/finance/services/finance-api";
import type { TransactionFilters } from "@/features/finance/types";

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

export function useSeedDemoFinanceData() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: seedDemoFinanceData,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}
