"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAdminAuditLogs,
  getAdminOperationsOverview,
  getDeadLetterJobs,
  getProviderDeliveryEvents,
  getWorkerQueue,
  getWorkerQueues,
  retryDeadLetterJob,
  retryWorkerQueue,
} from "@/features/admin/services/admin-api";

export const adminOperationsKey = ["admin", "operations"] as const;

export function useAdminOperationsOverview() {
  return useQuery({
    queryKey: [...adminOperationsKey, "overview"],
    queryFn: getAdminOperationsOverview,
    refetchInterval: 15_000,
  });
}

export function useWorkerQueues(filters: { status?: string; queueName?: string; search?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "queues", filters],
    queryFn: () => getWorkerQueues({ ...filters, limit: 100 }),
    refetchInterval: 15_000,
  });
}

export function useWorkerQueue(jobId?: string | null) {
  return useQuery({
    queryKey: [...adminOperationsKey, "queue", jobId],
    queryFn: () => getWorkerQueue(jobId as string),
    enabled: Boolean(jobId),
    refetchInterval: 15_000,
  });
}

export function useDeadLetterJobs(search?: string) {
  return useQuery({
    queryKey: [...adminOperationsKey, "dead-letter", search],
    queryFn: () => getDeadLetterJobs(search),
    refetchInterval: 15_000,
  });
}

export function useProviderDeliveryEvents(filters: { provider?: string; status?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "provider-events", filters],
    queryFn: () => getProviderDeliveryEvents({ ...filters, limit: 80 }),
    refetchInterval: 20_000,
  });
}

export function useAdminAuditLogs() {
  return useQuery({
    queryKey: [...adminOperationsKey, "audit-logs"],
    queryFn: getAdminAuditLogs,
    refetchInterval: 30_000,
  });
}

export function useRetryWorkerQueue() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobId, reason }: { jobId: string; reason?: string }) => retryWorkerQueue(jobId, reason),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: adminOperationsKey });
    },
  });
}

export function useRetryDeadLetterJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ deadLetterId, reason }: { deadLetterId: string; reason?: string }) =>
      retryDeadLetterJob(deadLetterId, reason),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: adminOperationsKey });
    },
  });
}
