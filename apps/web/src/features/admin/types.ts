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

export type OperationalAlert = {
  id: string;
  alertKey: string;
  severity: "INFO" | "WARNING" | "CRITICAL" | string;
  status: "ACTIVE" | "ACKNOWLEDGED" | "RESOLVED" | string;
  title: string;
  summary: string;
  sourceType: string;
  sourceId?: string | null;
  runbookSlug?: string | null;
  firstSeenAt: string;
  lastSeenAt: string;
  acknowledgedAt?: string | null;
  acknowledgedByEmail?: string | null;
  acknowledgmentNote?: string | null;
  resolvedAt?: string | null;
  metadataJson?: string | null;
};

export type IncidentLog = {
  id: string;
  incidentKey: string;
  severity: "WARNING" | "CRITICAL" | string;
  status: "OPEN" | "RESOLVED" | string;
  title: string;
  summary: string;
  primarySourceType: string;
  primarySourceId?: string | null;
  alertCount: number;
  openedAt: string;
  lastEventAt: string;
  acknowledgedAt?: string | null;
  resolvedAt?: string | null;
  metadataJson?: string | null;
};

export type RunbookEntry = {
  id: string;
  slug: string;
  title: string;
  severity: string;
  category: string;
  summary: string;
  symptoms: string;
  diagnosisSteps: string;
  mitigationSteps: string;
  escalationNotes?: string | null;
  relatedAlertKeys?: string | null;
  updatedAt: string;
};

export type ProviderWebhookEvent = {
  id: string;
  provider: string;
  channel: string;
  eventType: string;
  normalizedStatus: string;
  providerEventId?: string | null;
  providerMessageId?: string | null;
  signatureValid: boolean;
  duplicateEvent: boolean;
  replayOfEventId?: string | null;
  deliverySynced: boolean;
  notificationDeliveryId?: string | null;
  failureReason?: string | null;
  receivedAt: string;
  processedAt?: string | null;
};

export type DeliveryTimelineEvent = {
  id: string;
  notificationDeliveryId?: string | null;
  source: string;
  provider: string;
  eventType: string;
  status: string;
  message: string;
  observedAt: string;
};

export type ReliabilityOverview = {
  status: string;
  observedAt: string;
  alerts: OperationalAlert[];
  incidents: IncidentLog[];
  webhookEvents: ProviderWebhookEvent[];
  runbooks: RunbookEntry[];
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

export type OperationalTraceEvent = {
  id: string;
  eventType: string;
  severity: "INFO" | "WARNING" | "CRITICAL" | string;
  environment: string;
  releaseVersion?: string | null;
  releaseCommit?: string | null;
  source: string;
  sourceId?: string | null;
  traceId?: string | null;
  message: string;
  metadataJson?: string | null;
  observedAt: string;
};
