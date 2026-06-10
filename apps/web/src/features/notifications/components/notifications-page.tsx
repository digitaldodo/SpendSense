"use client";

import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  Bell,
  CalendarClock,
  CheckCheck,
  Inbox,
  Loader2,
  Repeat2,
  Settings2,
  ShieldCheck,
  Trash2,
} from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import type { Notification, ScheduledReport } from "@/features/finance/types";
import {
  useCreateScheduledReport,
  useDeleteScheduledReport,
  useDismissNotification,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotificationPreferences,
  useNotificationSummary,
  useNotifications,
  useReportDeliveryLogs,
  useScheduledReports,
  useUpdateNotificationPreferences,
  useUpdateScheduledReport,
} from "@/features/notifications/hooks/use-notifications";
import { cn } from "@/lib/utils";

type Tab = "inbox" | "recurring" | "reports" | "preferences";

export function NotificationsPage({ initialTab = "inbox" }: { initialTab?: Tab }) {
  const searchParams = useSearchParams();
  const requestedTab = searchParams.get("tab") as Tab | null;
  const [tab, setTab] = useState<Tab>(requestedTab ?? initialTab);
  const summaryQuery = useNotificationSummary();
  const notificationsQuery = useNotifications();
  const markAllRead = useMarkAllNotificationsRead();
  const notifications = notificationsQuery.data ?? [];
  const summary = summaryQuery.data;

  return (
    <main className="grid gap-5">
      <section className="grid gap-4 rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6 lg:grid-cols-[1fr_auto] lg:items-center">
        <div className="space-y-2">
          <div className="inline-flex w-fit items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <Bell className="size-4 text-primary" aria-hidden />
            Calm engagement
          </div>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">Notifications and scheduled reports</h2>
          <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
            Deterministic reminders for budgets, recurring payments, reports, and savings momentum.
          </p>
        </div>
        <div className="grid gap-2 sm:grid-cols-2 lg:min-w-72">
          <MiniMetric label="Unread" value={`${summary?.unreadCount ?? 0}`} />
          <MiniMetric label="Active" value={`${summary?.activeCount ?? 0}`} />
        </div>
      </section>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="grid grid-cols-2 gap-2 rounded-lg border border-border bg-card p-1 shadow-raised sm:flex">
          <TabButton active={tab === "inbox"} onClick={() => setTab("inbox")} icon={<Inbox className="size-4" />}>
            Inbox
          </TabButton>
          <TabButton active={tab === "recurring"} onClick={() => setTab("recurring")} icon={<Repeat2 className="size-4" />}>
            Recurring
          </TabButton>
          <TabButton active={tab === "reports"} onClick={() => setTab("reports")} icon={<CalendarClock className="size-4" />}>
            Reports
          </TabButton>
          <TabButton active={tab === "preferences"} onClick={() => setTab("preferences")} icon={<Settings2 className="size-4" />}>
            Preferences
          </TabButton>
        </div>
        <Button
          variant="outline"
          disabled={markAllRead.isPending || (summary?.unreadCount ?? 0) === 0}
          onClick={() => markAllRead.mutate()}
        >
          {markAllRead.isPending ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <CheckCheck className="size-4" aria-hidden />}
          Mark all read
        </Button>
      </div>

      {notificationsQuery.isLoading || summaryQuery.isLoading ? (
        <NotificationLoading />
      ) : tab === "inbox" ? (
        <InboxView notifications={notifications} />
      ) : tab === "recurring" ? (
        <RecurringReview notifications={notifications.filter((item) => item.type === "RECURRING_PAYMENT_DUE")} />
      ) : tab === "reports" ? (
        <ScheduledReportsView />
      ) : (
        <PreferencesView />
      )}
    </main>
  );
}

function InboxView({ notifications }: { notifications: Notification[] }) {
  const timeline = useMemo(() => notifications.slice(0, 40), [notifications]);
  if (timeline.length === 0) {
    return (
      <EmptyState
        icon={<ShieldCheck className="size-7 text-primary" aria-hidden />}
        title="Nothing needs your attention"
        body="New reminders appear here when deterministic thresholds are met."
      />
    );
  }
  return (
    <section className="grid gap-4 xl:grid-cols-[1fr_0.75fr]">
      <div className="grid gap-3">
        {timeline.map((notification) => (
          <NotificationCard key={notification.id} notification={notification} />
        ))}
      </div>
      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader>
          <CardTitle className="text-base">Financial event timeline</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3">
          {timeline.slice(0, 10).map((item) => (
            <div key={`${item.id}-timeline`} className="grid grid-cols-[auto_1fr] gap-3">
              <span className={cn("mt-1 size-2.5 rounded-full", severityDot(item.severity))} aria-hidden />
              <div className="min-w-0 border-b border-border/70 pb-3 last:border-b-0">
                <p className="truncate text-sm font-medium">{item.title}</p>
                <p className="mt-1 text-xs text-muted-foreground">{formatDateTime(item.createdAt)}</p>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </section>
  );
}

function RecurringReview({ notifications }: { notifications: Notification[] }) {
  if (notifications.length === 0) {
    return (
      <EmptyState
        icon={<Repeat2 className="size-7 text-primary" aria-hidden />}
        title="No upcoming recurring payments"
        body="Detected subscriptions and bills will appear when a due date is within the reminder window."
      />
    );
  }
  return (
    <section className="grid gap-4 lg:grid-cols-2">
      {notifications.map((notification) => (
        <Card key={notification.id} className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <div>
              <CardTitle className="text-base">{notification.title}</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">{notification.body}</p>
            </div>
            <Repeat2 className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="flex flex-col gap-2 sm:flex-row">
            <NotificationActions notification={notification} />
          </CardContent>
        </Card>
      ))}
    </section>
  );
}

function ScheduledReportsView() {
  const schedulesQuery = useScheduledReports();
  const logsQuery = useReportDeliveryLogs();
  const createSchedule = useCreateScheduledReport();
  const deleteSchedule = useDeleteScheduledReport();
  const [reportType, setReportType] = useState("MONTHLY_SUMMARY");
  const [format, setFormat] = useState("PDF");
  const [cadence, setCadence] = useState("MONTHLY");
  const [timezone, setTimezone] = useState("Asia/Kolkata");

  function submit(event: FormEvent) {
    event.preventDefault();
    createSchedule.mutate({ reportType, format, cadence, timezone, deliveryChannel: "IN_APP", active: true });
  }

  return (
    <section className="grid gap-4 xl:grid-cols-[0.85fr_1.15fr]">
      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader>
          <CardTitle className="text-base">Schedule export</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="grid gap-3" onSubmit={submit}>
            <FilterSelect value={reportType} onChange={setReportType}>
              <option value="MONTHLY_SUMMARY">Monthly summary</option>
              <option value="CATEGORY_REPORT">Category report</option>
            </FilterSelect>
            <FilterSelect value={format} onChange={setFormat}>
              <option value="PDF">PDF</option>
              <option value="CSV">CSV</option>
            </FilterSelect>
            <FilterSelect value={cadence} onChange={setCadence}>
              <option value="MONTHLY">Monthly</option>
              <option value="WEEKLY">Weekly</option>
            </FilterSelect>
            <Input value={timezone} onChange={(event) => setTimezone(event.target.value)} />
            <Button disabled={createSchedule.isPending}>
              {createSchedule.isPending ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <CalendarClock className="size-4" aria-hidden />}
              Schedule report
            </Button>
          </form>
        </CardContent>
      </Card>

      <div className="grid gap-4">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Scheduled exports</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            {schedulesQuery.isLoading ? (
              <Skeleton className="h-24 w-full" />
            ) : (schedulesQuery.data ?? []).length === 0 ? (
              <p className="rounded-lg border border-dashed border-border bg-muted/20 p-4 text-sm text-muted-foreground">
                No report schedules yet.
              </p>
            ) : (
              (schedulesQuery.data ?? []).map((schedule) => (
                <ScheduledReportRow
                  key={schedule.id}
                  schedule={schedule}
                  onPause={() => deleteSchedule.mutate(schedule.id)}
                />
              ))
            )}
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Delivery audit</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {(logsQuery.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">Delivery logs appear after scheduled exports are generated.</p>
            ) : (
              (logsQuery.data ?? []).slice(0, 8).map((log) => (
                <div key={log.id} className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/60 px-3 py-2 text-sm">
                  <span>{log.status}</span>
                  <span className="text-xs text-muted-foreground">{formatDateTime(log.attemptedAt)}</span>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </div>
    </section>
  );
}

function ScheduledReportRow({ schedule, onPause }: { schedule: ScheduledReport; onPause: () => void }) {
  const updateSchedule = useUpdateScheduledReport(schedule.id);
  return (
    <div className="grid gap-3 rounded-lg border border-border/70 bg-background/65 p-3 sm:grid-cols-[1fr_auto] sm:items-center">
      <div className="min-w-0">
        <p className="font-medium">{schedule.reportType.replace("_", " ").toLowerCase()}</p>
        <p className="mt-1 text-sm text-muted-foreground">
          {schedule.cadence.toLowerCase()} {schedule.format} in {schedule.timezone}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">Next run: {formatDateTime(schedule.nextRunAt)}</p>
      </div>
      <div className="flex gap-2">
        <Button
          size="sm"
          variant="outline"
          disabled={updateSchedule.isPending}
          onClick={() =>
            updateSchedule.mutate({
              reportType: schedule.reportType,
              format: schedule.format,
              cadence: schedule.cadence,
              timezone: schedule.timezone,
              deliveryChannel: schedule.deliveryChannel,
              active: !schedule.active,
            })
          }
        >
          {schedule.active ? "Pause" : "Resume"}
        </Button>
        <Button size="icon-sm" variant="ghost" title="Remove schedule" onClick={onPause}>
          <Trash2 className="size-4" aria-hidden />
        </Button>
      </div>
    </div>
  );
}

function PreferencesView() {
  const preferencesQuery = useNotificationPreferences();
  const updatePreferences = useUpdateNotificationPreferences();
  const preferences = preferencesQuery.data;

  if (preferencesQuery.isLoading || !preferences) {
    return <Skeleton className="h-80 w-full" />;
  }

  return (
    <section className="grid gap-4 lg:grid-cols-2">
      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader>
          <CardTitle className="text-base">Notification preferences</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3">
          <PreferenceToggle label="In-app notifications" value={preferences.inAppEnabled} onChange={(value) => updatePreferences.mutate({ inAppEnabled: value })} />
          <PreferenceToggle label="Budget warnings" value={preferences.budgetWarningsEnabled} onChange={(value) => updatePreferences.mutate({ budgetWarningsEnabled: value })} />
          <PreferenceToggle label="Recurring payment reminders" value={preferences.recurringRemindersEnabled} onChange={(value) => updatePreferences.mutate({ recurringRemindersEnabled: value })} />
          <PreferenceToggle label="Report ready notices" value={preferences.reportReadyEnabled} onChange={(value) => updatePreferences.mutate({ reportReadyEnabled: value })} />
          <PreferenceToggle label="Savings nudges" value={preferences.savingsNudgesEnabled} onChange={(value) => updatePreferences.mutate({ savingsNudgesEnabled: value })} />
          <PreferenceToggle label="Spending increase notices" value={preferences.spendingIncreaseEnabled} onChange={(value) => updatePreferences.mutate({ spendingIncreaseEnabled: value })} />
        </CardContent>
      </Card>
      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader>
          <CardTitle className="text-base">Report cadence defaults</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3">
          <PreferenceToggle label="Weekly digest" value={preferences.weeklyDigestEnabled} onChange={(value) => updatePreferences.mutate({ weeklyDigestEnabled: value })} />
          <PreferenceToggle label="Monthly report" value={preferences.monthlyReportEnabled} onChange={(value) => updatePreferences.mutate({ monthlyReportEnabled: value })} />
          <label className="grid gap-1 text-sm">
            <span className="font-medium">Timezone</span>
            <Input
              defaultValue={preferences.timezone}
              onBlur={(event) => updatePreferences.mutate({ timezone: event.target.value || "Asia/Kolkata" })}
            />
          </label>
          <div className="grid gap-2 sm:grid-cols-2">
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Quiet start</span>
              <Input type="time" defaultValue={preferences.quietHoursStart ?? ""} onBlur={(event) => updatePreferences.mutate({ quietHoursStart: event.target.value || null })} />
            </label>
            <label className="grid gap-1 text-sm">
              <span className="font-medium">Quiet end</span>
              <Input type="time" defaultValue={preferences.quietHoursEnd ?? ""} onBlur={(event) => updatePreferences.mutate({ quietHoursEnd: event.target.value || null })} />
            </label>
          </div>
        </CardContent>
      </Card>
    </section>
  );
}

function NotificationCard({ notification }: { notification: Notification }) {
  return (
    <div
      className={cn(
        "grid gap-3 rounded-lg border border-border bg-card p-4 shadow-raised transition duration-200 hover:-translate-y-0.5 hover:shadow-card sm:grid-cols-[auto_1fr_auto]",
        !notification.read && "border-primary/35 bg-primary/5"
      )}
    >
      <span className={cn("mt-1 size-3 rounded-full", severityDot(notification.severity))} aria-hidden />
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-semibold">{notification.title}</h3>
          {!notification.read ? <span className="rounded-lg bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">Unread</span> : null}
        </div>
        <p className="mt-1 text-sm leading-6 text-muted-foreground">{notification.body}</p>
        <p className="mt-2 text-xs text-muted-foreground">{formatDateTime(notification.createdAt)}</p>
      </div>
      <NotificationActions notification={notification} />
    </div>
  );
}

function NotificationActions({ notification }: { notification: Notification }) {
  const markRead = useMarkNotificationRead();
  const dismiss = useDismissNotification();
  return (
    <div className="flex flex-wrap gap-2 sm:justify-end">
      {notification.actionUrl && notification.actionLabel ? (
        <Button size="sm" variant="outline" render={<Link href={notification.actionUrl} />}>
          {notification.actionLabel}
        </Button>
      ) : null}
      {!notification.read ? (
        <Button size="icon-sm" variant="ghost" title="Mark read" disabled={markRead.isPending} onClick={() => markRead.mutate(notification.id)}>
          <CheckCheck className="size-4" aria-hidden />
        </Button>
      ) : null}
      <Button size="icon-sm" variant="ghost" title="Dismiss" disabled={dismiss.isPending} onClick={() => dismiss.mutate(notification.id)}>
        <Trash2 className="size-4" aria-hidden />
      </Button>
    </div>
  );
}

function PreferenceToggle({ label, value, onChange }: { label: string; value: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/65 px-3 py-2 text-sm">
      <span className="font-medium">{label}</span>
      <input
        className="size-5 accent-primary"
        type="checkbox"
        checked={value}
        onChange={(event) => onChange(event.target.checked)}
      />
    </label>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/65 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function EmptyState({ icon, title, body }: { icon: ReactNode; title: string; body: string }) {
  return (
    <div className="grid min-h-64 place-items-center rounded-lg border border-dashed border-border bg-muted/20 p-6 text-center">
      <div className="grid max-w-sm gap-3">
        <span className="mx-auto">{icon}</span>
        <h3 className="font-semibold">{title}</h3>
        <p className="text-sm leading-6 text-muted-foreground">{body}</p>
      </div>
    </div>
  );
}

function NotificationLoading() {
  return (
    <div className="grid gap-3">
      <Skeleton className="h-28 w-full" />
      <Skeleton className="h-28 w-full" />
      <Skeleton className="h-28 w-full" />
    </div>
  );
}

function TabButton({ active, children, icon, onClick }: { active: boolean; children: ReactNode; icon: ReactNode; onClick: () => void }) {
  return (
    <button
      className={cn(
        "flex h-10 items-center justify-center gap-2 rounded-lg px-3 text-sm font-medium transition-colors",
        active ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
      )}
      type="button"
      onClick={onClick}
    >
      {icon}
      {children}
    </button>
  );
}

function FilterSelect({
  children,
  value,
  onChange,
}: {
  children: ReactNode;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <select
      className="h-10 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50"
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      {children}
    </select>
  );
}

function severityDot(severity: string) {
  if (severity === "ACTION") {
    return "bg-info";
  }
  if (severity === "CAUTION") {
    return "bg-warning";
  }
  return "bg-success";
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "Not scheduled";
  }
  return new Date(value).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}
