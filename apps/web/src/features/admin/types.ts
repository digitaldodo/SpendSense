import type { WorkerJobLog } from "@/features/finance/types";

export type DeliveryAnalytics = {
  deliveriesLast24h: number;
  deliveredLast24h: number;
  failedLast24h: number;
  retryScheduled: number;
  successRate: number;
  retryExhaustedLast24h: number;
  averageProviderLatencyMs?: number | null;
};

export type QueueHealth = {
  queueName: string;
  ready: number;
  running: number;
  retryScheduled: number;
  deadLetter: number;
  lagSeconds: number;
  throughputLastHour: number;
  failuresLastHour: number;
};

export type ProviderStatus = {
  provider: string;
  channel: string;
  status: string;
  attemptsLast24h: number;
  failuresLast24h: number;
  successRate: number;
  averageLatencyMs?: number | null;
  lastEventAt?: string | null;
  lastErrorCode?: string | null;
};

export type AdminNotification = {
  id: string;
  severity: "INFO" | "CAUTION" | "ACTION" | string;
  title: string;
  body: string;
  targetType: string;
  targetId?: string | null;
  createdAt: string;
};

export type AdminOperationsOverview = {
  status: string;
  observedAt: string;
  deliveryAnalytics: DeliveryAnalytics;
  queues: QueueHealth[];
  providers: ProviderStatus[];
  notifications: AdminNotification[];
  recentJobs: WorkerJobLog[];
};

export type WorkerQueue = {
  id: string;
  queueName: string;
  jobType: string;
  status: string;
  priority: number;
  scheduledFor: string;
  lockedBy?: string | null;
  lockedUntil?: string | null;
  attemptCount: number;
  maxAttempts: number;
  payloadJson: string;
  traceId?: string | null;
  lastErrorCode?: string | null;
  lastErrorMessage?: string | null;
  enqueuedAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  failedAt?: string | null;
  latencyMs: number;
  createdAt: string;
  updatedAt: string;
};

export type DeadLetterJob = {
  id: string;
  workerQueueId?: string | null;
  queueName: string;
  jobType: string;
  failedStatus: string;
  attemptCount: number;
  payloadJson: string;
  failureCode?: string | null;
  failureMessage?: string | null;
  traceId?: string | null;
  exhaustedAt: string;
  retriedFromDeadLetterAt?: string | null;
  createdAt: string;
};

export type ProviderDeliveryEvent = {
  id: string;
  notificationDeliveryId?: string | null;
  provider: string;
  channel: string;
  eventType: string;
  status: string;
  providerMessageId?: string | null;
  latencyMs?: number | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  observedAt: string;
};

export type AdminAuditLog = {
  id: string;
  actorUserId?: string | null;
  actorEmail?: string | null;
  action: string;
  targetType: string;
  targetId?: string | null;
  reason?: string | null;
  traceId?: string | null;
  createdAt: string;
};
