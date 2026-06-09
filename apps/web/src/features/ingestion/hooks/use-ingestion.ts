"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  confirmCsvImport,
  getImportFailures,
  getImportHistory,
  previewCsvImport,
} from "@/features/ingestion/services/ingestion-api";

export const importHistoryQueryKey = ["imports", "history"] as const;

export function usePreviewCsvImport() {
  return useMutation({
    mutationFn: previewCsvImport,
  });
}

export function useConfirmCsvImport() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: confirmCsvImport,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
      queryClient.invalidateQueries({ queryKey: ["imports"] });
    },
  });
}

export function useImportHistory() {
  return useQuery({
    queryKey: importHistoryQueryKey,
    queryFn: getImportHistory,
  });
}

export function useImportFailures(jobId?: string | null) {
  return useQuery({
    queryKey: ["imports", "failures", jobId],
    queryFn: () => getImportFailures(jobId as string),
    enabled: Boolean(jobId),
  });
}
