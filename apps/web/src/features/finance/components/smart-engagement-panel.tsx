"use client";

import type { ComponentType } from "react";
import {
  Bell,
  CalendarCheck2,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Flame,
  HeartHandshake,
  LineChart,
  Moon,
  ShieldCheck,
  Sparkles,
  Target,
  Trophy,
  X,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useCompleteSmartAction,
  useCompleteWeeklyCheckIn,
  useDismissSmartAction,
  useSmartActionDashboard,
  useSnoozeSmartAction,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type { SmartAction } from "@/features/finance/types";
import { cn } from "@/lib/utils";

export function SmartEngagementPanel() {
  const dashboardQuery = useSmartActionDashboard();
  const completeAction = useCompleteSmartAction();
  const dismissAction = useDismissSmartAction();
  const snoozeAction = useSnoozeSmartAction();
  const completeWeekly = useCompleteWeeklyCheckIn();
  const dashboard = dashboardQuery.data;

  if (dashboardQuery.isLoading) {
    return <SmartEngagementLoading />;
  }

  if (!dashboard) {
    return null;
  }

  function complete(actionId: string) {
    completeAction.mutate({ actionId, payload: { reason: "Completed from smart action center" } });
  }

  function dismiss(actionId: string) {
    dismissAction.mutate({ actionId, payload: { reason: "Dismissed from smart action center" } });
  }

  function snooze(actionId: string) {
    const snoozedUntil = new Date(Date.now() + 3 * 24 * 60 * 60_000).toISOString();
    snoozeAction.mutate({ actionId, payload: { reason: "Snoozed from smart action center", snoozedUntil } });
  }

  const primaryAction = dashboard.actions.find((action) => action.id === dashboard.todayFocus.actionId);

  return (
    <section className="grid gap-4" id="smart-actions">
      <section className="grid gap-4 xl:grid-cols-[1.08fr_0.92fr]">
        <Card className="overflow-hidden rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <div>
              <CardTitle className="text-base">Daily financial summary</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">{dashboard.dailySummary.headline}</p>
            </div>
            <Sparkles className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-4">
            <div className="grid gap-3 sm:grid-cols-3">
              <MetricTile label="Income" value={formatMoney(dashboard.dailySummary.monthIncome)} />
              <MetricTile label="Spent" value={formatMoney(dashboard.dailySummary.monthSpend)} />
              <MetricTile
                label="Cashflow"
                value={formatMoney(dashboard.dailySummary.netCashflow)}
                state={dashboard.dailySummary.netCashflow >= 0 ? "HEALTHY" : "CAUTION"}
              />
            </div>
            <div className="grid gap-2">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">Savings consistency</span>
                <span className="font-medium tabular-nums">
                  {Math.round(dashboard.dailySummary.savingsRate)}%
                </span>
              </div>
              <CalmProgress value={Math.max(0, dashboard.dailySummary.savingsRate)} state={dashboard.dailySummary.tone} />
              <p className="text-xs leading-5 text-muted-foreground">{dashboard.dailySummary.explanation}</p>
            </div>
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <div>
              <CardTitle className="text-base">Today&apos;s financial focus</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">{dashboard.todayFocus.body}</p>
            </div>
            <Target className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-4">
            <div className="rounded-lg border border-border/70 bg-muted/20 p-4">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-semibold">{dashboard.todayFocus.title}</h3>
                <Badge variant="outline">{focusLabel(dashboard.todayFocus.focusType)}</Badge>
              </div>
              <p className="mt-3 text-2xl font-semibold tabular-nums">
                {dashboard.todayFocus.impactAmount > 0
                  ? formatMoney(dashboard.todayFocus.impactAmount)
                  : "Steady"}
              </p>
            </div>
            {primaryAction ? (
              <div className="flex flex-col gap-2 sm:flex-row">
                <Button
                  className="w-full sm:w-fit"
                  disabled={completeAction.isPending || primaryAction.status === "COMPLETED"}
                  onClick={() => complete(primaryAction.id)}
                >
                  <CheckCircle2 className="size-4" aria-hidden />
                  {primaryAction.status === "COMPLETED" ? "Completed" : "Mark done"}
                </Button>
                <Button className="w-full sm:w-fit" variant="outline" onClick={() => snooze(primaryAction.id)}>
                  <Clock3 className="size-4" aria-hidden />
                  Later
                </Button>
              </div>
            ) : null}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <div>
              <CardTitle className="text-base">Smart action center</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">Grounded recommendations from your ledger.</p>
            </div>
            <HeartHandshake className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-3">
            {dashboard.actions.length === 0 ? (
              <EmptyLine icon={ShieldCheck} title="No action pressure" body="Current patterns do not need a new recommendation." />
            ) : (
              dashboard.actions.map((action) => (
                <ActionRow
                  key={action.id}
                  action={action}
                  completing={completeAction.isPending}
                  dismissing={dismissAction.isPending}
                  snoozing={snoozeAction.isPending}
                  onComplete={complete}
                  onDismiss={dismiss}
                  onSnooze={snooze}
                />
              ))
            )}
          </CardContent>
        </Card>

        <div className="grid gap-4">
          <Card className="rounded-lg border-border shadow-raised">
            <CardHeader className="flex flex-row items-start justify-between gap-3">
              <div>
                <CardTitle className="text-base">Habit streaks</CardTitle>
                <p className="mt-1 text-sm text-muted-foreground">Momentum without pressure.</p>
              </div>
              <Flame className="size-5 text-primary" aria-hidden />
            </CardHeader>
            <CardContent className="grid gap-3">
              {dashboard.streaks.map((streak) => (
                <div key={streak.id} className="rounded-lg border border-border/70 bg-background/60 p-3 transition-colors hover:bg-muted/35">
                  <div className="flex items-center justify-between gap-3">
                    <p className="min-w-0 truncate text-sm font-medium">{streak.label}</p>
                    <span className="text-lg font-semibold tabular-nums">
                      {streak.currentCount}
                      <span className="ml-1 text-xs font-medium text-muted-foreground">{streak.unit}</span>
                    </span>
                  </div>
                  <CalmProgress value={Math.min(100, streak.currentCount * 12)} state={streak.state} className="mt-3" />
                  <p className="mt-2 text-xs leading-5 text-muted-foreground">{streak.explanation}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="rounded-lg border-border shadow-raised">
            <CardHeader className="flex flex-row items-start justify-between gap-3">
              <div>
                <CardTitle className="text-base">Weekly check-in</CardTitle>
                <p className="mt-1 text-sm text-muted-foreground">{dashboard.weeklyCheckIn.headline}</p>
              </div>
              <CalendarCheck2 className="size-5 text-primary" aria-hidden />
            </CardHeader>
            <CardContent className="grid gap-3">
              <CheckInList items={dashboard.weeklyCheckIn.wins} icon={Trophy} />
              <CheckInList items={dashboard.weeklyCheckIn.focus} icon={ChevronRight} />
              <Button
                className="w-full"
                variant={dashboard.weeklyCheckIn.status === "COMPLETED" ? "outline" : "default"}
                disabled={completeWeekly.isPending || dashboard.weeklyCheckIn.status === "COMPLETED"}
                onClick={() => completeWeekly.mutate()}
              >
                <CheckCircle2 className="size-4" aria-hidden />
                {dashboard.weeklyCheckIn.status === "COMPLETED" ? "Checked in" : "Complete check-in"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <CardTitle className="text-base">Financial wins</CardTitle>
            <Trophy className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-2">
            {dashboard.milestones.map((milestone) => (
              <MiniStory key={milestone.type} title={milestone.title} body={milestone.body} state={milestone.state} />
            ))}
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <CardTitle className="text-base">Smart reminders</CardTitle>
            <Bell className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-2">
            {dashboard.reminders.slice(0, 4).map((reminder) => (
              <MiniStory key={`${reminder.type}-${reminder.actionId ?? reminder.title}`} title={reminder.title} body={reminder.body} state={reminder.state} />
            ))}
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-row items-start justify-between gap-3">
            <CardTitle className="text-base">Improvement journey</CardTitle>
            <LineChart className="size-5 text-primary" aria-hidden />
          </CardHeader>
          <CardContent className="grid gap-3">
            <div className="flex items-end justify-between gap-3">
              <div>
                <p className="text-3xl font-semibold tabular-nums">{dashboard.journey.score}</p>
                <p className="text-xs text-muted-foreground">{dashboard.journey.state.toLowerCase()}</p>
              </div>
              <CalmRing value={dashboard.journey.score} />
            </div>
            <p className="text-xs leading-5 text-muted-foreground">{dashboard.journey.headline}</p>
            {dashboard.journey.steps.map((step) => (
              <div key={step.label} className="grid gap-1.5">
                <div className="flex items-center justify-between gap-3 text-sm">
                  <span className="font-medium">{step.label}</span>
                  <span className="tabular-nums text-muted-foreground">{step.progress}%</span>
                </div>
                <CalmProgress value={step.progress} state={step.state} />
              </div>
            ))}
          </CardContent>
        </Card>
      </section>

      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader className="flex flex-row items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Spending behavior timeline</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">Monthly cashflow movement from posted data.</p>
          </div>
          <LineChart className="size-5 text-primary" aria-hidden />
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
            {dashboard.behaviorTimeline.map((item) => (
              <div key={item.label} className="rounded-lg border border-border/70 bg-background/60 p-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-medium text-muted-foreground">{item.label}</span>
                  <span className={cn("size-2 rounded-full", dotClass(item.state))} aria-hidden />
                </div>
                <p className="mt-2 text-sm font-semibold tabular-nums">{formatMoney(item.value)}</p>
                <p className="mt-1 text-xs leading-5 text-muted-foreground">{item.body}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </section>
  );
}

function ActionRow({
  action,
  completing,
  dismissing,
  snoozing,
  onComplete,
  onDismiss,
  onSnooze,
}: {
  action: SmartAction;
  completing: boolean;
  dismissing: boolean;
  snoozing: boolean;
  onComplete: (actionId: string) => void;
  onDismiss: (actionId: string) => void;
  onSnooze: (actionId: string) => void;
}) {
  const isCompleted = action.status === "COMPLETED";
  return (
    <div
      className={cn(
        "grid gap-3 rounded-lg border border-border/70 bg-background/70 p-3 transition-all duration-200 hover:-translate-y-0.5 hover:bg-muted/35",
        isCompleted && "border-success/35 bg-success/10"
      )}
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={isCompleted ? "default" : "outline"}>{statusLabel(action.status)}</Badge>
            <span className="text-xs text-muted-foreground">{focusLabel(action.actionType)}</span>
          </div>
          <h3 className="mt-2 text-sm font-semibold">{action.title}</h3>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{action.body}</p>
        </div>
        <div className="shrink-0 text-left sm:text-right">
          <p className="text-base font-semibold tabular-nums">
            {action.impactAmount > 0 ? formatMoney(action.impactAmount, action.currency) : `${Math.round(action.impactPercent)}%`}
          </p>
          <p className="text-xs text-muted-foreground">impact</p>
        </div>
      </div>
      <p className="rounded-lg bg-muted/35 px-3 py-2 text-xs leading-5 text-muted-foreground">{action.explanation}</p>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Button
          className="w-full sm:w-fit"
          size="sm"
          disabled={isCompleted || completing}
          onClick={() => onComplete(action.id)}
        >
          <CheckCircle2 className="size-4" aria-hidden />
          {isCompleted ? "Done" : "Done"}
        </Button>
        <Button className="w-full sm:w-fit" size="sm" variant="outline" disabled={isCompleted || snoozing} onClick={() => onSnooze(action.id)}>
          <Moon className="size-4" aria-hidden />
          Snooze
        </Button>
        <Button className="w-full sm:w-fit" size="sm" variant="ghost" disabled={isCompleted || dismissing} onClick={() => onDismiss(action.id)}>
          <X className="size-4" aria-hidden />
          Dismiss
        </Button>
      </div>
    </div>
  );
}

function MetricTile({ label, value, state }: { label: string; value: string; state?: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/60 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn("mt-1 truncate text-sm font-semibold tabular-nums", stateTextClass(state))}>{value}</p>
    </div>
  );
}

function CalmProgress({ value, state, className }: { value: number; state?: string; className?: string }) {
  return (
    <div className={cn("h-2 overflow-hidden rounded-full bg-muted", className)}>
      <div
        className={cn("h-full rounded-full transition-[width] duration-500", fillClass(state))}
        style={{ width: `${Math.min(Math.max(value, 4), 100)}%` }}
      />
    </div>
  );
}

function CalmRing({ value }: { value: number }) {
  const normalized = Math.min(Math.max(value, 0), 100);
  return (
    <svg className="size-14" viewBox="0 0 44 44" role="img" aria-label="Journey score">
      <circle cx="22" cy="22" r="17" fill="none" stroke="var(--muted)" strokeWidth="6" />
      <circle
        cx="22"
        cy="22"
        r="17"
        fill="none"
        pathLength="100"
        stroke="var(--primary)"
        strokeDasharray={`${normalized} ${100 - normalized}`}
        strokeLinecap="round"
        strokeWidth="6"
        transform="rotate(-90 22 22)"
      />
    </svg>
  );
}

function CheckInList({
  items,
  icon: Icon,
}: {
  items: string[];
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
}) {
  return (
    <div className="grid gap-2">
      {items.slice(0, 3).map((item) => (
        <div key={item} className="flex items-start gap-2 rounded-lg border border-border/70 bg-background/60 px-3 py-2 text-sm">
          <Icon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden />
          <span className="leading-5 text-muted-foreground">{item}</span>
        </div>
      ))}
    </div>
  );
}

function MiniStory({ title, body, state }: { title: string; body: string; state: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/60 px-3 py-2">
      <div className="flex items-center gap-2">
        <span className={cn("size-2 rounded-full", dotClass(state))} aria-hidden />
        <p className="truncate text-sm font-medium">{title}</p>
      </div>
      <p className="mt-1 text-xs leading-5 text-muted-foreground">{body}</p>
    </div>
  );
}

function EmptyLine({
  icon: Icon,
  title,
  body,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  title: string;
  body: string;
}) {
  return (
    <div className="grid gap-2 rounded-lg border border-dashed border-border bg-muted/20 p-5 text-center">
      <Icon className="mx-auto size-6 text-primary" aria-hidden />
      <p className="text-sm font-semibold">{title}</p>
      <p className="text-sm text-muted-foreground">{body}</p>
    </div>
  );
}

function SmartEngagementLoading() {
  return (
    <section className="grid gap-4 xl:grid-cols-2" aria-hidden="true">
      <Skeleton className="h-56 w-full" />
      <Skeleton className="h-56 w-full" />
    </section>
  );
}

function focusLabel(value: string) {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^\w/, (letter) => letter.toUpperCase());
}

function statusLabel(value: string) {
  if (value === "OPEN") return "Open";
  if (value === "SNOOZED") return "Snoozed";
  if (value === "COMPLETED") return "Complete";
  return value.toLowerCase();
}

function fillClass(state?: string) {
  if (state === "CAUTION" || state === "RECOVERY" || state === "BUILDING") return "bg-warning";
  if (state === "RISK") return "bg-info";
  return "bg-success";
}

function dotClass(state?: string) {
  if (state === "CAUTION" || state === "RECOVERY" || state === "BUILDING" || state === "OPEN") return "bg-warning";
  if (state === "RISK" || state === "SNOOZED") return "bg-info";
  return "bg-success";
}

function stateTextClass(state?: string) {
  if (state === "CAUTION" || state === "RECOVERY" || state === "BUILDING") return "text-warning";
  if (state === "RISK") return "text-info";
  if (state === "HEALTHY") return "text-success";
  return "";
}
