"use client";

import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import {
  Activity,
  Bell,
  BookOpen,
  CheckCircle2,
  Clock3,
  DatabaseZap,
  ExternalLink,
  MailCheck,
  RefreshCw,
  RotateCcw,
  Search,
  ServerCog,
  ShieldCheck,
  Siren,
  TimerReset,
  Webhook,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import type {
  AdminNotification,
  DeadLetterJob,
  IncidentLog,
  OperationalAlert,
  OperationalTraceEvent,
  ProviderDeliveryEvent,
  ProviderWebhookEvent,
  QueueHealth,
  RunbookEntry,
  WorkerQueue,
} from "@/features/admin/types";
import {
  useAcknowledgeOperationalAlert,
  useAdminAuditLogs,
  useAdminOperationsOverview,
  useDeadLetterJobs,
  useDeliveryTimeline,
  useIncidents,
  useOperationalAlerts,
  useOperationalTraceEvents,
  useProviderDeliveryEvents,
  useProviderWebhookEvents,
  useReliabilityOverview,
  useRetryDeadLetterJob,
  useRetryWorkerQueue,
  useRunbooks,
  useWorkerQueue,
  useWorkerQueues,
} from "@/features/admin/hooks/use-admin-operations";
import { cn } from "@/lib/utils";

const statuses = ["", "PENDING", "RUNNING", "RETRY_SCHEDULED", "DEAD_LETTER", "COMPLETED"] as const;

export function AdminOperationsPage() {
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const [runbookSearch, setRunbookSearch] = useState("");
  const [alertSeverity, setAlertSeverity] = useState("");
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [selectedIncident, setSelectedIncident] = useState<IncidentLog | null>(null);
  const [selectedDeliveryId, setSelectedDeliveryId] = useState<string | null>(null);
  const [eventStatus, setEventStatus] = useState("");
  const [traceSeverity, setTraceSeverity] = useState("");
  const overviewQuery = useAdminOperationsOverview();
  const reliabilityQuery = useReliabilityOverview();
  const alertsQuery = useOperationalAlerts({ status: "ACTIVE", severity: alertSeverity });
  const incidentsQuery = useIncidents({ status: "OPEN" });
  const webhooksQuery = useProviderWebhookEvents({});
  const runbooksQuery = useRunbooks({ search: runbookSearch });
  const queuesQuery = useWorkerQueues({ status, search });
  const deadLettersQuery = useDeadLetterJobs(search);
  const eventsQuery = useProviderDeliveryEvents({ status: eventStatus });
  const auditQuery = useAdminAuditLogs();
  const tracesQuery = useOperationalTraceEvents({ severity: traceSeverity });

  const overview = overviewQuery.data;
  const queues = useMemo(() => queuesQuery.data ?? [], [queuesQuery.data]);
  const deadLetters = deadLettersQuery.data ?? [];
  const providerEvents = eventsQuery.data ?? [];
  const traces = tracesQuery.data ?? [];
  const reliability = reliabilityQuery.data;
  const alerts = alertsQuery.data ?? reliability?.alerts ?? [];
  const incidents = incidentsQuery.data ?? reliability?.incidents ?? [];
  const webhookEvents = webhooksQuery.data ?? reliability?.webhookEvents ?? [];
  const runbooks = runbooksQuery.data ?? reliability?.runbooks ?? [];
  const failedJobs = useMemo(
    () => queues.filter((job) => job.status === "DEAD_LETTER" || job.status === "RETRY_SCHEDULED"),
    [queues]
  );

  function refreshAll() {
    overviewQuery.refetch();
    reliabilityQuery.refetch();
    alertsQuery.refetch();
    incidentsQuery.refetch();
    webhooksQuery.refetch();
    runbooksQuery.refetch();
    queuesQuery.refetch();
    deadLettersQuery.refetch();
    eventsQuery.refetch();
    auditQuery.refetch();
    tracesQuery.refetch();
  }

  if (overviewQuery.isLoading && !overview) {
    return <AdminLoading />;
  }

  return (
    <main className="grid gap-5">
      <section className="grid gap-4 rounded-lg border border-border bg-card p-5 shadow-raised lg:grid-cols-[1fr_auto] lg:items-center">
        <div className="space-y-2">
          <div className="inline-flex w-fit items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden />
            Admin operations
          </div>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">Delivery and worker monitoring</h2>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
            Queue execution, provider delivery health, retry controls, and audit-safe operations in one focused view.
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row lg:justify-end">
          <StatusPill status={overview?.status ?? "WAITING"} />
          <Button variant="outline" onClick={refreshAll}>
            <RefreshCw className="size-4" aria-hidden />
            Refresh
          </Button>
        </div>
      </section>

      <IncidentBanner incidents={incidents} status={reliability?.status ?? overview?.status ?? "WAITING"} onOpen={setSelectedIncident} />

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={<MailCheck className="size-5" />}
          label="24h delivery success"
          value={`${overview?.deliveryAnalytics.successRate ?? 100}%`}
          detail={`${overview?.deliveryAnalytics.deliveredLast24h ?? 0} delivered of ${overview?.deliveryAnalytics.deliveriesLast24h ?? 0}`}
        />
        <MetricCard
          icon={<TimerReset className="size-5" />}
          label="Retry queue"
          value={`${overview?.deliveryAnalytics.retryScheduled ?? 0}`}
          detail={`${overview?.deliveryAnalytics.retryExhaustedLast24h ?? 0} exhausted in 24h`}
        />
        <MetricCard
          icon={<Clock3 className="size-5" />}
          label="Provider latency"
          value={overview?.deliveryAnalytics.averageProviderLatencyMs ? `${overview.deliveryAnalytics.averageProviderLatencyMs}ms` : "Waiting"}
          detail="Average from provider events"
        />
        <MetricCard
          icon={<ServerCog className="size-5" />}
          label="Active alerts"
          value={`${alerts.length}`}
          detail={`${incidents.length} open incident group(s)`}
        />
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_0.9fr]">
        <QueueHealthPanel queues={overview?.queues ?? []} />
        <ProviderStatusPanel providers={overview?.providers ?? []} notifications={overview?.notifications ?? []} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <AlertCenter alerts={alerts} severity={alertSeverity} onSeverityChange={setAlertSeverity} />
        <IncidentPanel incidents={incidents} onOpen={setSelectedIncident} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="gap-3">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <CardTitle className="text-base">Worker and retry queue</CardTitle>
                <p className="mt-1 text-sm text-muted-foreground">Polling every 15 seconds; retry actions are audited.</p>
              </div>
              <div className="grid gap-2 sm:grid-cols-[10rem_1fr]">
                <select
                  className="h-10 rounded-lg border border-input bg-background px-3 text-sm"
                  value={status}
                  onChange={(event) => setStatus(event.target.value)}
                >
                  {statuses.map((item) => (
                    <option key={item || "ALL"} value={item}>
                      {item || "All statuses"}
                    </option>
                  ))}
                </select>
                <label className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
                  <Input
                    className="pl-9"
                    placeholder="Search jobs, payloads, errors"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </label>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <QueueTable jobs={queues} failedJobs={failedJobs} onSelect={setSelectedJobId} />
          </CardContent>
        </Card>

        <div className="grid gap-4">
          <DeadLetterPanel jobs={deadLetters} onSelectQueueJob={setSelectedJobId} />
          <AuditPanel auditLogs={auditQuery.data ?? []} />
        </div>
      </section>

      <section className="grid gap-4">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <CardTitle className="text-base">Provider delivery events</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">Send attempts, latency, provider errors, and fallback readiness.</p>
            </div>
            <select
              className="h-10 rounded-lg border border-input bg-background px-3 text-sm"
              value={eventStatus}
              onChange={(event) => setEventStatus(event.target.value)}
            >
              <option value="">All events</option>
              <option value="DELIVERED">Delivered</option>
              <option value="FAILED">Failed</option>
            </select>
          </CardHeader>
          <CardContent>
            <ProviderEventTable events={providerEvents} onOpenTimeline={setSelectedDeliveryId} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <WebhookTimelinePanel events={webhookEvents} onOpenTimeline={setSelectedDeliveryId} />
        <RunbookViewer runbooks={runbooks} search={runbookSearch} onSearchChange={setRunbookSearch} />
      </section>

      <OperationalTracePanel traces={traces} severity={traceSeverity} onSeverityChange={setTraceSeverity} />

      <JobDetailSheet jobId={selectedJobId} onOpenChange={(open) => !open && setSelectedJobId(null)} />
      <IncidentDetailSheet incident={selectedIncident} onOpenChange={(open) => !open && setSelectedIncident(null)} />
      <DeliveryTimelineSheet deliveryId={selectedDeliveryId} onOpenChange={(open) => !open && setSelectedDeliveryId(null)} />
    </main>
  );
}

function QueueHealthPanel({ queues }: { queues: QueueHealth[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Queue health</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {queues.length === 0 ? (
          <EmptyLine icon={<DatabaseZap className="size-5" />} text="No queue records yet." />
        ) : (
          queues.map((queue) => (
            <div key={queue.queueName} className="grid gap-3 rounded-lg border border-border/70 bg-background/65 p-3 sm:grid-cols-[1fr_auto] sm:items-center">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold">{queue.queueName}</p>
                  <StatusPill status={queue.deadLetter > 0 ? "DEGRADED" : queue.lagSeconds > 900 ? "CAUTION" : "OK"} compact />
                </div>
                <p className="mt-1 text-sm text-muted-foreground">
                  Lag {formatDuration(queue.lagSeconds)} · {queue.throughputLastHour} completed in the last hour
                </p>
              </div>
              <div className="grid grid-cols-4 gap-2 text-center text-xs">
                <MiniCount label="Ready" value={queue.ready} />
                <MiniCount label="Run" value={queue.running} />
                <MiniCount label="Retry" value={queue.retryScheduled} />
                <MiniCount label="DLQ" value={queue.deadLetter} />
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function IncidentBanner({ incidents, status, onOpen }: { incidents: IncidentLog[]; status: string; onOpen: (incident: IncidentLog) => void }) {
  const topIncident = incidents[0];
  if (!topIncident) {
    return (
      <section className="flex flex-col gap-3 rounded-lg border border-success/25 bg-success/10 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-success/15 text-success">
            <CheckCircle2 className="size-5" aria-hidden />
          </span>
          <div>
            <p className="font-semibold">No active incident</p>
            <p className="mt-1 text-sm text-muted-foreground">Reliability status is {status.toLowerCase()} with no open incident groups.</p>
          </div>
        </div>
      </section>
    );
  }
  return (
    <section className="flex flex-col gap-3 rounded-lg border border-warning/35 bg-warning/10 p-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-start gap-3">
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-warning/15 text-warning">
          <Siren className="size-5" aria-hidden />
        </span>
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-semibold">{topIncident.title}</p>
            <StatusPill status={topIncident.severity} compact />
          </div>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{topIncident.summary}</p>
        </div>
      </div>
      <Button variant="outline" onClick={() => onOpen(topIncident)}>
        <ExternalLink className="size-4" aria-hidden />
        Details
      </Button>
    </section>
  );
}

function AlertCenter({ alerts, severity, onSeverityChange }: { alerts: OperationalAlert[]; severity: string; onSeverityChange: (value: string) => void }) {
  const acknowledgeAlert = useAcknowledgeOperationalAlert();
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <CardTitle className="text-base">Alert center</CardTitle>
          <p className="mt-1 text-sm text-muted-foreground">Active operational alerts with acknowledgement and runbook pointers.</p>
        </div>
        <select
          className="h-10 rounded-lg border border-input bg-background px-3 text-sm"
          value={severity}
          onChange={(event) => onSeverityChange(event.target.value)}
        >
          <option value="">All severities</option>
          <option value="CRITICAL">Critical</option>
          <option value="WARNING">Warning</option>
          <option value="INFO">Info</option>
        </select>
      </CardHeader>
      <CardContent className="grid gap-3">
        {alerts.length === 0 ? (
          <EmptyLine icon={<CheckCircle2 className="size-5" />} text="No active operational alerts." />
        ) : (
          alerts.map((alert) => (
            <div key={alert.id} className="grid gap-3 rounded-lg border border-border/70 bg-background/70 p-3">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill status={alert.severity} compact />
                    <p className="font-semibold">{alert.title}</p>
                  </div>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">{alert.summary}</p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={acknowledgeAlert.isPending || alert.status !== "ACTIVE"}
                  onClick={() => acknowledgeAlert.mutate({ alertId: alert.id })}
                >
                  <CheckCircle2 className="size-4" aria-hidden />
                  Acknowledge
                </Button>
              </div>
              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>{alert.sourceType.replaceAll("_", " ")}</span>
                <span>Last seen {formatDateTime(alert.lastSeenAt)}</span>
                {alert.runbookSlug ? <span>Runbook {alert.runbookSlug.replaceAll("-", " ")}</span> : null}
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function IncidentPanel({ incidents, onOpen }: { incidents: IncidentLog[]; onOpen: (incident: IncidentLog) => void }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Incident groups</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2">
        {incidents.length === 0 ? (
          <EmptyLine icon={<ShieldCheck className="size-5" />} text="No open incidents." />
        ) : (
          incidents.map((incident) => (
            <button
              key={incident.id}
              type="button"
              className="grid gap-2 rounded-lg border border-border/70 bg-background/65 p-3 text-left transition hover:border-primary/45"
              onClick={() => onOpen(incident)}
            >
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-semibold">{incident.incidentKey.replaceAll("-", " ")}</p>
                <StatusPill status={incident.severity} compact />
              </div>
              <p className="text-xs leading-5 text-muted-foreground">{incident.summary}</p>
              <p className="text-xs text-muted-foreground">{incident.alertCount} alert(s) · {formatDateTime(incident.lastEventAt)}</p>
            </button>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function ProviderStatusPanel({ providers, notifications }: { providers: { provider: string; status: string; successRate: number; failuresLast24h: number; averageLatencyMs?: number | null; lastEventAt?: string | null }[]; notifications: AdminNotification[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="flex flex-row items-start justify-between gap-3">
        <div>
          <CardTitle className="text-base">Provider status and notifications</CardTitle>
          <p className="mt-1 text-sm text-muted-foreground">Outage signals are based on recent provider event failure rates.</p>
        </div>
        <Bell className="size-5 text-primary" aria-hidden />
      </CardHeader>
      <CardContent className="grid gap-3">
        <div className="grid gap-2">
          {providers.length === 0 ? (
            <EmptyLine icon={<MailCheck className="size-5" />} text="No provider events recorded yet." />
          ) : (
            providers.map((provider) => (
              <div key={provider.provider} className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/65 px-3 py-2">
                <div>
                  <p className="text-sm font-semibold">{provider.provider}</p>
                  <p className="text-xs text-muted-foreground">
                    {provider.successRate}% success · {provider.averageLatencyMs ?? "--"}ms avg
                  </p>
                </div>
                <StatusPill status={provider.status} compact />
              </div>
            ))
          )}
        </div>
        <div className="grid gap-2">
          {notifications.length === 0 ? (
            <EmptyLine icon={<ShieldCheck className="size-5" />} text="No admin notifications." />
          ) : (
            notifications.map((item) => (
              <div key={item.id} className="grid gap-1 rounded-lg border border-border/70 bg-muted/25 p-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold">{item.title}</p>
                  <StatusPill status={item.severity} compact />
                </div>
                <p className="text-xs leading-5 text-muted-foreground">{item.body}</p>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function QueueTable({ jobs, failedJobs, onSelect }: { jobs: WorkerQueue[]; failedJobs: WorkerQueue[]; onSelect: (id: string) => void }) {
  const retryQueue = useRetryWorkerQueue();
  if (jobs.length === 0) {
    return <EmptyLine icon={<ServerCog className="size-5" />} text="No queue jobs match the current filters." />;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="border-b border-border text-xs uppercase text-muted-foreground">
          <tr>
            <th className="py-2 pr-3 font-medium">Job</th>
            <th className="py-2 pr-3 font-medium">Status</th>
            <th className="py-2 pr-3 font-medium">Attempts</th>
            <th className="py-2 pr-3 font-medium">Scheduled</th>
            <th className="py-2 pr-3 font-medium">Error</th>
            <th className="py-2 text-right font-medium">Action</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/70">
          {jobs.map((job) => {
            const retryable = job.status === "DEAD_LETTER" || job.status === "RETRY_SCHEDULED";
            const highlighted = failedJobs.some((item) => item.id === job.id);
            return (
              <tr key={job.id} className={cn("align-top", highlighted && "bg-warning/5")}>
                <td className="py-3 pr-3">
                  <button className="text-left font-semibold hover:text-primary" type="button" onClick={() => onSelect(job.id)}>
                    {job.jobType}
                  </button>
                  <p className="mt-1 text-xs text-muted-foreground">{job.queueName}</p>
                </td>
                <td className="py-3 pr-3"><StatusPill status={job.status} compact /></td>
                <td className="py-3 pr-3 tabular-nums">{job.attemptCount}/{job.maxAttempts}</td>
                <td className="py-3 pr-3 text-muted-foreground">{formatDateTime(job.scheduledFor)}</td>
                <td className="max-w-72 py-3 pr-3 text-xs text-muted-foreground">
                  <span className="line-clamp-2">{job.lastErrorMessage ?? job.lastErrorCode ?? "None"}</span>
                </td>
                <td className="py-3 text-right">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!retryable || retryQueue.isPending}
                    onClick={() => retryQueue.mutate({ jobId: job.id })}
                  >
                    <RotateCcw className="size-4" aria-hidden />
                    Retry
                  </Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function DeadLetterPanel({ jobs, onSelectQueueJob }: { jobs: DeadLetterJob[]; onSelectQueueJob: (id: string) => void }) {
  const retryDeadLetter = useRetryDeadLetterJob();
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Dead-letter queue</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2">
        {jobs.length === 0 ? (
          <EmptyLine icon={<ShieldCheck className="size-5" />} text="No exhausted jobs." />
        ) : (
          jobs.slice(0, 8).map((job) => (
            <div key={job.id} className="grid gap-2 rounded-lg border border-warning/40 bg-warning/10 p-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{job.jobType}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{job.failureMessage ?? job.failureCode ?? "Exhausted retries"}</p>
                </div>
                <Button
                  size="icon-sm"
                  variant="ghost"
                  title="Retry dead-letter job"
                  disabled={retryDeadLetter.isPending}
                  onClick={() => retryDeadLetter.mutate({ deadLetterId: job.id })}
                >
                  <RotateCcw className="size-4" aria-hidden />
                </Button>
              </div>
              {job.workerQueueId ? (
                <button className="w-fit text-xs font-medium text-primary" type="button" onClick={() => onSelectQueueJob(job.workerQueueId as string)}>
                  Open linked queue job
                </button>
              ) : null}
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function ProviderEventTable({ events, onOpenTimeline }: { events: ProviderDeliveryEvent[]; onOpenTimeline: (deliveryId: string) => void }) {
  if (events.length === 0) {
    return <EmptyLine icon={<Activity className="size-5" />} text="Provider events will appear after delivery attempts." />;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] text-left text-sm">
        <thead className="border-b border-border text-xs uppercase text-muted-foreground">
          <tr>
            <th className="py-2 pr-3 font-medium">Provider</th>
            <th className="py-2 pr-3 font-medium">Status</th>
            <th className="py-2 pr-3 font-medium">Latency</th>
            <th className="py-2 pr-3 font-medium">Message</th>
            <th className="py-2 pr-3 font-medium">Observed</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border/70">
          {events.map((event) => (
            <tr key={event.id}>
              <td className="py-3 pr-3 font-medium">{event.provider}</td>
              <td className="py-3 pr-3"><StatusPill status={event.status} compact /></td>
              <td className="py-3 pr-3 tabular-nums">{event.latencyMs ? `${event.latencyMs}ms` : "--"}</td>
              <td className="max-w-80 py-3 pr-3 text-xs text-muted-foreground">
                <span className="line-clamp-2">{event.errorMessage ?? event.providerMessageId ?? event.eventType}</span>
              </td>
              <td className="py-3 pr-3 text-muted-foreground">
                <div className="flex items-center gap-2">
                  <span>{formatDateTime(event.observedAt)}</span>
                  {event.notificationDeliveryId ? (
                    <Button size="icon-xs" variant="ghost" title="Open delivery timeline" onClick={() => onOpenTimeline(event.notificationDeliveryId as string)}>
                      <Activity className="size-3" aria-hidden />
                    </Button>
                  ) : null}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function WebhookTimelinePanel({ events, onOpenTimeline }: { events: ProviderWebhookEvent[]; onOpenTimeline: (deliveryId: string) => void }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="flex flex-row items-start justify-between gap-3">
        <div>
          <CardTitle className="text-base">Webhook event timeline</CardTitle>
          <p className="mt-1 text-sm text-muted-foreground">Provider callbacks, replay detection, signature state, and delivery sync.</p>
        </div>
        <Webhook className="size-5 text-primary" aria-hidden />
      </CardHeader>
      <CardContent className="grid gap-3">
        {events.length === 0 ? (
          <EmptyLine icon={<Webhook className="size-5" />} text="Webhook callbacks will appear after providers post events." />
        ) : (
          events.map((event) => (
            <div key={event.id} className="grid gap-2 rounded-lg border border-border/70 bg-background/65 p-3">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusPill status={event.normalizedStatus} compact />
                  <p className="text-sm font-semibold">{event.provider} · {event.eventType.replaceAll("_", " ").toLowerCase()}</p>
                </div>
                <span className="text-xs text-muted-foreground">{formatDateTime(event.receivedAt)}</span>
              </div>
              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>{event.signatureValid ? "Signature verified" : "Signature failed"}</span>
                <span>{event.duplicateEvent ? "Replay marked" : "First seen"}</span>
                <span>{event.deliverySynced ? "Delivery synced" : event.failureReason ?? "No delivery sync"}</span>
              </div>
              {event.notificationDeliveryId ? (
                <button className="w-fit text-xs font-medium text-primary" type="button" onClick={() => onOpenTimeline(event.notificationDeliveryId as string)}>
                  Open delivery history
                </button>
              ) : null}
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function RunbookViewer({ runbooks, search, onSearchChange }: { runbooks: RunbookEntry[]; search: string; onSearchChange: (value: string) => void }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="gap-3">
        <div className="flex items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Runbooks</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">Searchable admin-only procedures for production reliability events.</p>
          </div>
          <BookOpen className="size-5 text-primary" aria-hidden />
        </div>
        <label className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
          <Input className="pl-9" placeholder="Search runbooks" value={search} onChange={(event) => onSearchChange(event.target.value)} />
        </label>
      </CardHeader>
      <CardContent className="grid gap-3">
        {runbooks.length === 0 ? (
          <EmptyLine icon={<BookOpen className="size-5" />} text="No runbooks match the current search." />
        ) : (
          runbooks.map((runbook) => (
            <details key={runbook.id} className="rounded-lg border border-border/70 bg-background/65 p-3">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3">
                <span>
                  <span className="block text-sm font-semibold">{runbook.title}</span>
                  <span className="mt-1 block text-xs text-muted-foreground">{runbook.category} · {runbook.severity.toLowerCase()}</span>
                </span>
                <StatusPill status={runbook.severity} compact />
              </summary>
              <div className="mt-3 grid gap-3 text-sm leading-6 text-muted-foreground">
                <p>{runbook.summary}</p>
                <RunbookBlock title="Symptoms" value={runbook.symptoms} />
                <RunbookBlock title="Diagnosis" value={runbook.diagnosisSteps} />
                <RunbookBlock title="Mitigation" value={runbook.mitigationSteps} />
                {runbook.escalationNotes ? <RunbookBlock title="Escalation" value={runbook.escalationNotes} /> : null}
              </div>
            </details>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function OperationalTracePanel({
  traces,
  severity,
  onSeverityChange,
}: {
  traces: OperationalTraceEvent[];
  severity: string;
  onSeverityChange: (value: string) => void;
}) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <CardTitle className="text-base">Operational traces</CardTitle>
          <p className="mt-1 text-sm text-muted-foreground">Deployment rehearsal, rollback, worker, and retry evidence.</p>
        </div>
        <select
          className="h-10 rounded-lg border border-input bg-background px-3 text-sm"
          value={severity}
          onChange={(event) => onSeverityChange(event.target.value)}
        >
          <option value="">All severities</option>
          <option value="CRITICAL">Critical</option>
          <option value="WARNING">Warning</option>
          <option value="INFO">Info</option>
        </select>
      </CardHeader>
      <CardContent className="grid gap-3">
        {traces.length === 0 ? (
          <EmptyLine icon={<Activity className="size-5" />} text="Trace events appear after startup, drills, retries, or deployment checks." />
        ) : (
          traces.slice(0, 12).map((trace) => (
            <div key={trace.id} className="grid gap-2 rounded-lg border border-border/70 bg-background/65 p-3">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusPill status={trace.severity} compact />
                    <p className="font-semibold">{trace.eventType.replaceAll("_", " ").toLowerCase()}</p>
                  </div>
                  <p className="mt-1 text-sm leading-6 text-muted-foreground">{trace.message}</p>
                </div>
                <span className="text-xs text-muted-foreground">{formatDateTime(trace.observedAt)}</span>
              </div>
              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>{trace.environment}</span>
                <span>{trace.source}</span>
                {trace.releaseCommit ? <span>{shortCommit(trace.releaseCommit)}</span> : null}
                {trace.traceId ? <span>{trace.traceId}</span> : null}
              </div>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function RunbookBlock({ title, value }: { title: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-muted/20 p-3">
      <p className="text-xs font-semibold uppercase text-foreground">{title}</p>
      <p className="mt-1">{value}</p>
    </div>
  );
}

function AuditPanel({ auditLogs }: { auditLogs: { id: string; action: string; actorEmail?: string | null; reason?: string | null; createdAt: string }[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Admin audit</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2">
        {auditLogs.length === 0 ? (
          <EmptyLine icon={<ShieldCheck className="size-5" />} text="Privileged actions will appear here." />
        ) : (
          auditLogs.slice(0, 6).map((log) => (
            <div key={log.id} className="rounded-lg border border-border/70 bg-background/65 px-3 py-2 text-sm">
              <div className="flex items-center justify-between gap-3">
                <span className="font-semibold">{log.action.replaceAll("_", " ").toLowerCase()}</span>
                <span className="text-xs text-muted-foreground">{formatDateTime(log.createdAt)}</span>
              </div>
              <p className="mt-1 text-xs text-muted-foreground">{log.actorEmail ?? "Admin"} · {log.reason ?? "No reason provided"}</p>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function JobDetailSheet({ jobId, onOpenChange }: { jobId: string | null; onOpenChange: (open: boolean) => void }) {
  const jobQuery = useWorkerQueue(jobId);
  const job = jobQuery.data;
  return (
    <Sheet open={Boolean(jobId)} onOpenChange={onOpenChange}>
      <SheetContent className="w-[92vw] overflow-auto sm:max-w-xl">
        <SheetHeader>
          <SheetTitle>Job detail</SheetTitle>
          <SheetDescription>Queue state, retry history fields, payload, and latest worker error.</SheetDescription>
        </SheetHeader>
        {!job ? (
          <div className="grid gap-3 p-4">
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : (
          <div className="grid gap-4 px-4 pb-4">
            <div className="grid gap-2 rounded-lg border border-border bg-background/65 p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="font-semibold">{job.jobType}</p>
                <StatusPill status={job.status} compact />
              </div>
              <p className="text-xs text-muted-foreground">{job.id}</p>
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <DetailLine label="Queue" value={job.queueName} />
              <DetailLine label="Attempts" value={`${job.attemptCount}/${job.maxAttempts}`} />
              <DetailLine label="Scheduled" value={formatDateTime(job.scheduledFor)} />
              <DetailLine label="Latency" value={job.latencyMs ? `${job.latencyMs}ms` : "Pending"} />
              <DetailLine label="Locked by" value={job.lockedBy ?? "Not locked"} />
              <DetailLine label="Trace" value={job.traceId ?? "None"} />
            </div>
            <div className="grid gap-2 rounded-lg border border-border bg-muted/25 p-3">
              <p className="text-sm font-semibold">Latest error</p>
              <p className="text-sm leading-6 text-muted-foreground">{job.lastErrorMessage ?? job.lastErrorCode ?? "No recorded error."}</p>
            </div>
            <div className="grid gap-2">
              <p className="text-sm font-semibold">Payload</p>
              <pre className="max-h-80 overflow-auto rounded-lg border border-border bg-foreground/[0.03] p-3 text-xs leading-5">
                {prettyJson(job.payloadJson)}
              </pre>
            </div>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}

function IncidentDetailSheet({ incident, onOpenChange }: { incident: IncidentLog | null; onOpenChange: (open: boolean) => void }) {
  return (
    <Sheet open={Boolean(incident)} onOpenChange={onOpenChange}>
      <SheetContent className="w-[92vw] overflow-auto sm:max-w-xl">
        <SheetHeader>
          <SheetTitle>Incident detail</SheetTitle>
          <SheetDescription>Aggregated operational alert context and timeline signals.</SheetDescription>
        </SheetHeader>
        {incident ? (
          <div className="grid gap-4 px-4 pb-4">
            <div className="grid gap-2 rounded-lg border border-border bg-background/65 p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="font-semibold">{incident.incidentKey.replaceAll("-", " ")}</p>
                <StatusPill status={incident.severity} compact />
              </div>
              <p className="text-sm leading-6 text-muted-foreground">{incident.summary}</p>
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <DetailLine label="Status" value={incident.status.toLowerCase()} />
              <DetailLine label="Alerts" value={`${incident.alertCount}`} />
              <DetailLine label="Opened" value={formatDateTime(incident.openedAt)} />
              <DetailLine label="Last event" value={formatDateTime(incident.lastEventAt)} />
              <DetailLine label="Source" value={incident.primarySourceType.replaceAll("_", " ")} />
              <DetailLine label="Source id" value={incident.primarySourceId ?? "Grouped signal"} />
            </div>
            <div className="rounded-lg border border-border bg-muted/25 p-3">
              <p className="text-sm font-semibold">Operational note</p>
              <p className="mt-1 text-sm leading-6 text-muted-foreground">
                Use the linked alert runbooks and event timelines before retrying jobs. Keep the incident open until the active alert count returns to zero.
              </p>
            </div>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}

function DeliveryTimelineSheet({ deliveryId, onOpenChange }: { deliveryId: string | null; onOpenChange: (open: boolean) => void }) {
  const timelineQuery = useDeliveryTimeline(deliveryId);
  const events = timelineQuery.data ?? [];
  return (
    <Sheet open={Boolean(deliveryId)} onOpenChange={onOpenChange}>
      <SheetContent className="w-[92vw] overflow-auto sm:max-w-xl">
        <SheetHeader>
          <SheetTitle>Delivery status history</SheetTitle>
          <SheetDescription>Provider attempts, webhook synchronization, and retry history for this delivery.</SheetDescription>
        </SheetHeader>
        <div className="grid gap-3 px-4 pb-4">
          {!deliveryId || timelineQuery.isLoading ? (
            <>
              <Skeleton className="h-16 w-full" />
              <Skeleton className="h-16 w-full" />
            </>
          ) : events.length === 0 ? (
            <EmptyLine icon={<Activity className="size-5" />} text="No timeline events have been recorded for this delivery." />
          ) : (
            events.map((event) => (
              <div key={`${event.source}-${event.id}`} className="grid gap-2 rounded-lg border border-border/70 bg-background/65 p-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-semibold">{event.source.replaceAll("_", " ")} · {event.provider}</p>
                  <StatusPill status={event.status} compact />
                </div>
                <p className="text-sm leading-6 text-muted-foreground">{event.message}</p>
                <p className="text-xs text-muted-foreground">{event.eventType.replaceAll("_", " ").toLowerCase()} · {formatDateTime(event.observedAt)}</p>
              </div>
            ))
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}

function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardContent className="flex items-start justify-between gap-4 p-4">
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-2 text-2xl font-semibold tabular-nums">{value}</p>
          <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
        </div>
        <span className="grid size-10 place-items-center rounded-lg bg-primary/10 text-primary">{icon}</span>
      </CardContent>
    </Card>
  );
}

function StatusPill({ status, compact = false }: { status: string; compact?: boolean }) {
  const normalized = status.toUpperCase();
  const tone =
    normalized === "OK" || normalized === "DELIVERED" || normalized === "COMPLETED"
      ? "bg-success/15 text-success"
      : normalized === "ACTION" || normalized === "DEGRADED" || normalized === "FAILED" || normalized === "DEAD_LETTER"
        ? "bg-warning/15 text-warning"
        : "bg-primary/10 text-primary";
  return (
    <span className={cn("inline-flex h-7 items-center rounded-lg px-2.5 text-xs font-semibold", tone, compact && "h-6 px-2")}>
      {normalized.replaceAll("_", " ").toLowerCase()}
    </span>
  );
}

function MiniCount({ label, value }: { label: string; value: number }) {
  return (
    <div className="min-w-14 rounded-lg border border-border/70 bg-card px-2 py-1.5">
      <p className="font-semibold tabular-nums">{value}</p>
      <p className="mt-0.5 text-muted-foreground">{label}</p>
    </div>
  );
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/65 p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 break-words text-sm font-medium">{value}</p>
    </div>
  );
}

function EmptyLine({ icon, text }: { icon: ReactNode; text: string }) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-dashed border-border bg-muted/20 p-4 text-sm text-muted-foreground">
      <span className="text-primary">{icon}</span>
      {text}
    </div>
  );
}

function AdminLoading() {
  return (
    <div className="grid gap-4">
      <Skeleton className="h-36 w-full" />
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
      </div>
      <Skeleton className="h-96 w-full" />
    </div>
  );
}

function prettyJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function formatDuration(seconds: number) {
  if (seconds < 60) {
    return `${seconds}s`;
  }
  if (seconds < 3600) {
    return `${Math.round(seconds / 60)}m`;
  }
  return `${Math.round(seconds / 3600)}h`;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Waiting";
  }
  return new Date(value).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function shortCommit(value: string) {
  return value.length > 12 ? value.slice(0, 12) : value;
}
