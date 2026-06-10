"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  acknowledgeOperationalAlert,
  getAdminAuditLogs,
  getAdminOperationsOverview,
  getDeliveryTimeline,
  getDeadLetterJobs,
  getIncidents,
  getOperationalAlerts,
  getOperationalTraceEvents,
  getProviderDeliveryEvents,
  getProviderWebhookEvents,
  getReliabilityOverview,
  getRunbooks,
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

export function useOperationalTraceEvents(filters: { eventType?: string; severity?: string; source?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "trace-events", filters],
    queryFn: () => getOperationalTraceEvents({ ...filters, limit: 80 }),
    refetchInterval: 20_000,
  });
}

export function useReliabilityOverview() {
  return useQuery({
    queryKey: [...adminOperationsKey, "reliability-overview"],
    queryFn: getReliabilityOverview,
    refetchInterval: 15_000,
  });
}

export function useOperationalAlerts(filters: { status?: string; severity?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "alerts", filters],
    queryFn: () => getOperationalAlerts({ ...filters, limit: 100 }),
    refetchInterval: 15_000,
  });
}

export function useIncidents(filters: { status?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "incidents", filters],
    queryFn: () => getIncidents({ ...filters, limit: 60 }),
    refetchInterval: 20_000,
  });
}

export function useRunbooks(filters: { search?: string; severity?: string; category?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "runbooks", filters],
    queryFn: () => getRunbooks({ ...filters, limit: 80 }),
    refetchInterval: 60_000,
  });
}

export function useProviderWebhookEvents(filters: { provider?: string; status?: string }) {
  return useQuery({
    queryKey: [...adminOperationsKey, "webhook-events", filters],
    queryFn: () => getProviderWebhookEvents({ ...filters, limit: 80 }),
    refetchInterval: 20_000,
  });
}

export function useDeliveryTimeline(deliveryId?: string | null) {
  return useQuery({
    queryKey: [...adminOperationsKey, "delivery-timeline", deliveryId],
    queryFn: () => getDeliveryTimeline(deliveryId as string),
    enabled: Boolean(deliveryId),
    refetchInterval: 15_000,
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

export function useAcknowledgeOperationalAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ alertId, note }: { alertId: string; note?: string }) => acknowledgeOperationalAlert(alertId, note),
    onSuccess() {
      queryClient.invalidateQueries({ queryKey: adminOperationsKey });
    },
  });
}
