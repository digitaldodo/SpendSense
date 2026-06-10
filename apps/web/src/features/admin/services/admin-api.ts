"use client";

import { authenticatedApiClient } from "@/services/api/authenticated-client";
import type { ApiResponse } from "@/types/api";
import type {
  AdminAuditLog,
  AdminOperationsOverview,
  DeadLetterJob,
  DeliveryTimelineEvent,
  IncidentLog,
  OperationalAlert,
  OperationalTraceEvent,
  ProviderDeliveryEvent,
  ProviderWebhookEvent,
  ReliabilityOverview,
  RunbookEntry,
  WorkerQueue,
} from "@/features/admin/types";

type QueueFilters = {
  status?: string;
  queueName?: string;
  search?: string;
  limit?: number;
};

type ProviderEventFilters = {
  provider?: string;
  status?: string;
  limit?: number;
};

type ReliabilityFilters = {
  status?: string;
  severity?: string;
  provider?: string;
  search?: string;
  category?: string;
  eventType?: string;
  source?: string;
  limit?: number;
};

function queryString(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      params.set(key, String(value));
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function getAdminOperationsOverview() {
  const response = await authenticatedApiClient<ApiResponse<AdminOperationsOverview>>(
    "/api/v1/admin/operations/overview"
  );
  return response.data;
}

export async function getWorkerQueues(filters: QueueFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<WorkerQueue[]>>(
    `/api/v1/admin/operations/queues${queryString(filters)}`
  );
  return response.data;
}

export async function getWorkerQueue(jobId: string) {
  const response = await authenticatedApiClient<ApiResponse<WorkerQueue>>(
    `/api/v1/admin/operations/queues/${jobId}`
  );
  return response.data;
}

export async function retryWorkerQueue(jobId: string, reason?: string) {
  const response = await authenticatedApiClient<ApiResponse<WorkerQueue>>(
    `/api/v1/admin/operations/queues/${jobId}/retry`,
    {
      method: "POST",
      body: { reason: reason || "Admin retry from operations dashboard" },
    }
  );
  return response.data;
}

export async function getDeadLetterJobs(search?: string) {
  const response = await authenticatedApiClient<ApiResponse<DeadLetterJob[]>>(
    `/api/v1/admin/operations/dead-letter${queryString({ search, limit: 80 })}`
  );
  return response.data;
}

export async function retryDeadLetterJob(deadLetterId: string, reason?: string) {
  const response = await authenticatedApiClient<ApiResponse<DeadLetterJob>>(
    `/api/v1/admin/operations/dead-letter/${deadLetterId}/retry`,
    {
      method: "POST",
      body: { reason: reason || "Admin retry from dead-letter queue" },
    }
  );
  return response.data;
}

export async function getProviderDeliveryEvents(filters: ProviderEventFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<ProviderDeliveryEvent[]>>(
    `/api/v1/admin/operations/provider-events${queryString(filters)}`
  );
  return response.data;
}

export async function getAdminAuditLogs() {
  const response = await authenticatedApiClient<ApiResponse<AdminAuditLog[]>>(
    "/api/v1/admin/operations/audit-logs?limit=40"
  );
  return response.data;
}

export async function getOperationalTraceEvents(filters: ReliabilityFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<OperationalTraceEvent[]>>(
    `/api/v1/admin/operations/trace-events${queryString(filters)}`
  );
  return response.data;
}

export async function getReliabilityOverview() {
  const response = await authenticatedApiClient<ApiResponse<ReliabilityOverview>>(
    "/api/v1/admin/operations/reliability/overview"
  );
  return response.data;
}

export async function getOperationalAlerts(filters: ReliabilityFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<OperationalAlert[]>>(
    `/api/v1/admin/operations/alerts${queryString(filters)}`
  );
  return response.data;
}

export async function acknowledgeOperationalAlert(alertId: string, note?: string) {
  const response = await authenticatedApiClient<ApiResponse<OperationalAlert>>(
    `/api/v1/admin/operations/alerts/${alertId}/acknowledge`,
    {
      method: "POST",
      body: { note: note || "Acknowledged from reliability center" },
    }
  );
  return response.data;
}

export async function getIncidents(filters: ReliabilityFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<IncidentLog[]>>(
    `/api/v1/admin/operations/incidents${queryString(filters)}`
  );
  return response.data;
}

export async function getRunbooks(filters: ReliabilityFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<RunbookEntry[]>>(
    `/api/v1/admin/operations/runbooks${queryString(filters)}`
  );
  return response.data;
}

export async function getProviderWebhookEvents(filters: ReliabilityFilters = {}) {
  const response = await authenticatedApiClient<ApiResponse<ProviderWebhookEvent[]>>(
    `/api/v1/admin/operations/webhook-events${queryString(filters)}`
  );
  return response.data;
}

export async function getDeliveryTimeline(deliveryId: string) {
  const response = await authenticatedApiClient<ApiResponse<DeliveryTimelineEvent[]>>(
    `/api/v1/admin/operations/deliveries/${deliveryId}/timeline`
  );
  return response.data;
}
