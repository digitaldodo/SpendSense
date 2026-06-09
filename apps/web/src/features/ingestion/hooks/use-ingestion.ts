"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  confirmCsvImport,
  deleteSavedImportMapping,
  getImportDetail,
  getImportFailures,
  getImportHistory,
  getImportReconciliation,
  getSavedImportMappings,
  previewCsvImport,
  renameSavedImportMapping,
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

export function useImportDetail(jobId?: string | null) {
  return useQuery({
    queryKey: ["imports", "detail", jobId],
    queryFn: () => getImportDetail(jobId as string),
    enabled: Boolean(jobId),
  });
}

export function useImportReconciliation(jobId?: string | null) {
  return useQuery({
    queryKey: ["imports", "reconciliation", jobId],
    queryFn: () => getImportReconciliation(jobId as string),
    enabled: Boolean(jobId),
  });
}

export function useSavedImportMappings() {
  return useQuery({
    queryKey: ["imports", "mappings"],
    queryFn: getSavedImportMappings,
  });
}

export function useRenameSavedImportMapping() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ mappingId, name }: { mappingId: string; name: string }) =>
      renameSavedImportMapping(mappingId, name),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["imports"] });
    },
  });
}

export function useDeleteSavedImportMapping() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteSavedImportMapping,
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: ["imports"] });
    },
  });
}
