"use client";

import { useEffect, useMemo, useState } from "react";
import type { ComponentType, FormEvent, ReactNode } from "react";
import {
  Activity,
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Bell,
  CalendarClock,
  CheckCircle2,
  CircleDollarSign,
  Edit3,
  FileUp,
  Landmark,
  LinkIcon,
  Loader2,
  Merge,
  Mail,
  PiggyBank,
  Plus,
  ReceiptText,
  Repeat2,
  Search,
  ShieldCheck,
  Target,
  Trash2,
  TrendingUp,
  WalletCards,
} from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { FinancialCard } from "@/features/finance/components/financial-card";
import { SmartEngagementPanel } from "@/features/finance/components/smart-engagement-panel";
import { TransactionList } from "@/features/finance/components/transaction-list";
import {
  useAccounts,
  useAddGoalContribution,
  useBudgetHistory,
  useBulkUpdateTransactions,
  useCategories,
  useCreateBudget,
  useCreateCategory,
  useCreateSavingsGoal,
  useDashboardFinanceSummary,
  useDeleteBudget,
  useDeleteSavingsGoal,
  useMergeCategory,
  useSeedDemoFinanceData,
  useTransactions,
  useUpdateBudget,
  useUpdateCategory,
  useUpdateSavingsGoal,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type {
  Budget,
  CategorySpend,
  CategoryTrend,
  DashboardFinanceSummary,
  MonthlySummary,
  SavingsGoal,
  TransactionDirection,
  TransactionStatus,
} from "@/features/finance/types";
import {
  useNotificationPreferences,
  useSystemStatus,
} from "@/features/notifications/hooks/use-notifications";
import { cn } from "@/lib/utils";

const pageSize = 12;
const emptyBudgetOverview = {
  totalBudgeted: 0,
  totalSpent: 0,
  totalRemaining: 0,
  usagePercent: 0,
  overspentCount: 0,
  state: "HEALTHY" as const,
  budgets: [],
};
const emptyFinancialHealth = {
  score: 0,
  state: "HEALTHY" as const,
  savingsRatio: 0,
  spendingConsistency: 0,
  incomeExpenseStability: 0,
  overspendingFrequency: 0,
};
const emptySavingsMomentum = {
  monthNetSavings: 0,
  state: "HEALTHY" as const,
  savingsRatio: 0,
  goalContributionsThisMonth: 0,
};

export function DashboardOverview() {
  const summaryQuery = useDashboardFinanceSummary();
  const summary = summaryQuery.data;

  if (summaryQuery.isLoading) {
    return <DashboardLoadingState />;
  }

  if (!summary || summary.transactionCount === 0) {
    return <DashboardEmptyState accountCount={summary?.accountCount ?? 0} />;
  }

  return (
    <main className="grid gap-5" id="transactions">
      <section className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                {summary.demoSeeded ? "Demo data present" : "Real imported data"}
              </p>
              <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
                Your money view is based on imported transactions.
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                Spending, income, balances, categories, and imports are calculated from your ledger.
              </p>
            </div>
            <Button variant="outline" render={<Link href="/imports" />}>
              <FileUp className="size-4" aria-hidden />
              Upload
            </Button>
          </div>
        </div>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Recent import activity
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {summary.recentImports.length === 0 ? (
              <WidgetLine label="CSV imports" value="No imports yet" />
            ) : (
              summary.recentImports.slice(0, 3).map((job) => (
                <Link
                  key={job.id}
                  className="grid gap-1 rounded-lg border border-border/70 bg-background/60 px-3 py-2 text-sm transition-colors hover:bg-muted/45"
                  href={`/imports/${job.id}`}
                >
                  <span className="truncate font-medium">{job.originalFilename}</span>
                  <span className="text-xs text-muted-foreground">
                    {job.recordsImported} rows, {job.recordsDuplicate} skipped
                  </span>
                </Link>
              ))
            )}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <FinancialCard
          title="Account balance"
          value={formatMoney(summary.totalBalance)}
          detail={`${summary.accountCount} account(s) included.`}
          icon={Landmark}
        />
        <FinancialCard
          title="This month spent"
          value={formatMoney(summary.monthSpend)}
          detail="Posted debits in the current month."
          icon={ReceiptText}
        />
        <FinancialCard
          title="This month received"
          value={formatMoney(summary.monthIncome)}
          detail="Posted credits in the current month."
          icon={CircleDollarSign}
        />
        <FinancialCard
          title="Net cashflow"
          value={formatMoney(summary.netCashflow)}
          detail={
            summary.netCashflow >= 0
              ? "Income is ahead this month."
              : "Expenses are ahead this month."
          }
          icon={TrendingUp}
        />
      </section>

      <DashboardInsightStrip summary={summary} />

      <SmartEngagementPanel />

      <DeferredDashboardWorkspace summary={summary} />
    </main>
  );
}

function DeferredDashboardWorkspace({ summary }: { summary: DashboardFinanceSummary }) {
  const ready = useDeferredMount();

  if (!ready) {
    return <DashboardDeferredPlaceholder />;
  }

  return (
    <>
      <NotificationDashboardWidgets summary={summary} />

      <PlanningWorkspace summary={summary} />

      <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Spending trend</CardTitle>
          </CardHeader>
          <CardContent>
            <TrendLineChart data={summary.monthlySummary} />
          </CardContent>
        </Card>
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Category spending</CardTitle>
          </CardHeader>
          <CardContent>
            <CategoryDistribution data={summary.categoryBreakdown} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Monthly comparison</CardTitle>
          </CardHeader>
          <CardContent>
            <MonthlyComparison data={summary.monthlySummary} />
          </CardContent>
        </Card>
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Balances by account</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {summary.accounts.map((account) => (
              <WidgetLine
                key={account.id}
                label={account.displayName}
                value={formatMoney(account.currentBalance, account.currency)}
              />
            ))}
          </CardContent>
        </Card>
      </section>

      <CategoryManagementPanel />

      <TransactionExplorer />
    </>
  );
}

function useDeferredMount() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (ready) {
      return undefined;
    }

    let timeoutId: number | undefined;
    let idleId: number | undefined;
    let animationId: number | undefined;

    const reveal = () => setReady(true);

    const browserWindow = window as Window &
      typeof globalThis & {
        requestIdleCallback?: (callback: IdleRequestCallback, options?: IdleRequestOptions) => number;
        cancelIdleCallback?: (handle: number) => void;
      };

    if (browserWindow.requestIdleCallback) {
      idleId = browserWindow.requestIdleCallback(reveal, { timeout: 900 });
    } else {
      animationId = browserWindow.requestAnimationFrame(() => {
        timeoutId = browserWindow.setTimeout(reveal, 180);
      });
    }

    return () => {
      if (idleId !== undefined && browserWindow.cancelIdleCallback) {
        browserWindow.cancelIdleCallback(idleId);
      }
      if (animationId !== undefined) {
        browserWindow.cancelAnimationFrame(animationId);
      }
      if (timeoutId !== undefined) {
        browserWindow.clearTimeout(timeoutId);
      }
    };
  }, [ready]);

  return ready;
}

function DashboardInsightStrip({ summary }: { summary: DashboardFinanceSummary }) {
  const insight = summary.insightSummary;
  return (
    <section className="grid gap-4 lg:grid-cols-[1fr_auto] lg:items-center">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <InsightMiniCard
          icon={Repeat2}
          label="Subscriptions"
          value={`${insight.subscriptionCount}`}
          detail={formatMoney(insight.subscriptionSpend)}
        />
        <InsightMiniCard
          icon={Activity}
          label="Spikes"
          value={`${insight.spendingSpikeCount}`}
          detail="This range"
        />
        <InsightMiniCard
          icon={TrendingUp}
          label="MoM spend"
          value={`${Math.round(insight.monthOverMonthExpenseChangePercent)}%`}
          detail="Expense change"
        />
        <InsightMiniCard
          icon={CircleDollarSign}
          label="Income"
          value={stateLabelForInsight(insight.incomeConsistencyState)}
          detail="Consistency"
        />
        <InsightMiniCard
          icon={PiggyBank}
          label="Savings"
          value={insight.savingsTrendState.toLowerCase()}
          detail={insight.largestExpenseChangeCategory}
        />
      </div>
      <Button className="w-full lg:w-fit" variant="outline" render={<Link href="/insights" />}>
        <BarChart3 className="size-4" aria-hidden />
        Insights
      </Button>
    </section>
  );
}

function NotificationDashboardWidgets({ summary }: { summary: DashboardFinanceSummary }) {
  const notifications = summary.notificationDashboard;
  const preferencesQuery = useNotificationPreferences();
  const systemStatusQuery = useSystemStatus();
  const preferences = preferencesQuery.data;
  const systemStatus = systemStatusQuery.data;
  return (
    <section className="grid gap-4 xl:grid-cols-4">
      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader className="flex flex-row items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Upcoming subscriptions</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">
              Detected recurring payments due soon.
            </p>
          </div>
          <Repeat2 className="size-5 text-primary" aria-hidden />
        </CardHeader>
        <CardContent className="grid gap-2">
          {notifications.upcomingSubscriptions.length === 0 ? (
            <WidgetLine label="Recurring payments" value="Clear" />
          ) : (
            notifications.upcomingSubscriptions.slice(0, 3).map((item) => (
              <Link
                key={item.id}
                className="rounded-lg border border-border/70 bg-background/60 px-3 py-2 transition-colors hover:bg-muted/45"
                href="/notifications?tab=recurring"
              >
                <p className="truncate text-sm font-medium">{item.title}</p>
                <p className="mt-1 max-h-9 overflow-hidden text-xs text-muted-foreground">
                  {item.body}
                </p>
              </Link>
            ))
          )}
          <Button
            className="mt-1 w-full"
            variant="outline"
            render={<Link href="/notifications?tab=recurring" />}
          >
            <Repeat2 className="size-4" aria-hidden />
            Review recurring payments
          </Button>
        </CardContent>
      </Card>

      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader className="flex flex-row items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Budget warnings</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">
              Gentle threshold reminders from active budgets.
            </p>
          </div>
          <Bell className="size-5 text-primary" aria-hidden />
        </CardHeader>
        <CardContent className="grid gap-2">
          {notifications.budgetWarnings.length === 0 ? (
            <WidgetLine label="Budget thresholds" value="Steady" />
          ) : (
            notifications.budgetWarnings.slice(0, 3).map((item) => (
              <div
                key={item.id}
                className="rounded-lg border border-border/70 bg-background/60 px-3 py-2"
              >
                <p className="truncate text-sm font-medium">{item.title}</p>
                <p className="mt-1 text-xs text-muted-foreground">{item.body}</p>
              </div>
            ))
          )}
          <Button className="mt-1 w-full" variant="outline" render={<Link href="/notifications" />}>
            <Bell className="size-4" aria-hidden />
            Open reminders
          </Button>
        </CardContent>
      </Card>

      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader className="flex flex-row items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Scheduled reports</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">
              Exports queued for weekly or monthly cadence.
            </p>
          </div>
          <CalendarClock className="size-5 text-primary" aria-hidden />
        </CardHeader>
        <CardContent className="grid gap-2">
          {notifications.scheduledReports.length === 0 ? (
            <WidgetLine label="Report schedule" value="Not set" />
          ) : (
            notifications.scheduledReports
              .slice(0, 3)
              .map((schedule) => (
                <WidgetLine
                  key={schedule.id}
                  label={`${schedule.cadence.toLowerCase()} ${schedule.format}`}
                  value={new Date(schedule.nextRunAt).toLocaleDateString("en-IN", {
                    day: "numeric",
                    month: "short",
                  })}
                />
              ))
          )}
          {notifications.savingsNudges[0] ? (
            <div className="rounded-lg border border-border/70 bg-background/60 px-3 py-2">
              <p className="truncate text-sm font-medium">{notifications.savingsNudges[0].title}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                {notifications.savingsNudges[0].body}
              </p>
            </div>
          ) : null}
          <Button
            className="mt-1 w-full"
            variant="outline"
            render={<Link href="/notifications?tab=reports" />}
          >
            <CalendarClock className="size-4" aria-hidden />
            Manage exports
          </Button>
        </CardContent>
      </Card>

      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader className="flex flex-row items-start justify-between gap-3">
          <div>
            <CardTitle className="text-base">Delivery status</CardTitle>
            <p className="mt-1 text-sm text-muted-foreground">
              Reports, digests, and retry health.
            </p>
          </div>
          <Mail className="size-5 text-primary" aria-hidden />
        </CardHeader>
        <CardContent className="grid gap-2">
          <WidgetLine
            label="Report delivery"
            value={notifications.scheduledReports.length > 0 ? "Scheduled" : "Not set"}
          />
          <WidgetLine
            label="Digest"
            value={preferences?.digestFrequency?.toLowerCase() ?? "loading"}
          />
          <WidgetLine label="Email channel" value={preferences?.emailEnabled ? "On" : "Off"} />
          <WidgetLine label="Pending retries" value={`${systemStatus?.pendingRetries ?? 0}`} />
          <Button
            className="mt-1 w-full"
            variant="outline"
            render={<Link href="/notifications?tab=delivery" />}
          >
            <Mail className="size-4" aria-hidden />
            Delivery history
          </Button>
        </CardContent>
      </Card>
    </section>
  );
}

function InsightMiniCard({
  icon: Icon,
  label,
  value,
  detail,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-card p-3 shadow-raised">
      <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
        <Icon className="size-4 text-primary" aria-hidden />
        {label}
      </div>
      <p className="mt-2 truncate text-base font-semibold tabular-nums">{value}</p>
      <p className="mt-1 truncate text-xs text-muted-foreground">{detail}</p>
    </div>
  );
}

function TransactionExplorer() {
  const accountsQuery = useAccounts();
  const categoriesQuery = useCategories();
  const bulkUpdate = useBulkUpdateTransactions();
  const [search, setSearch] = useState("");
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [direction, setDirection] = useState<TransactionDirection | "">("");
  const [status, setStatus] = useState<TransactionStatus | "">("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [sort, setSort] = useState("-occurredAt");
  const [page, setPage] = useState(0);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [bulkCategoryId, setBulkCategoryId] = useState("");
  const [bulkStatus, setBulkStatus] = useState<TransactionStatus | "">("");

  const filters = useMemo(
    () => ({
      search,
      accountId,
      categoryId,
      direction: direction || undefined,
      status: status || undefined,
      from: from ? new Date(`${from}T00:00:00.000Z`).toISOString() : undefined,
      to: to ? new Date(`${to}T23:59:59.999Z`).toISOString() : undefined,
      page,
      size: pageSize,
      sort,
    }),
    [accountId, categoryId, direction, from, page, search, sort, status, to]
  );
  const transactionsQuery = useTransactions(filters);
  const transactionPage = transactionsQuery.data;

  function resetPage(update: () => void) {
    update();
    setPage(0);
    setSelectedIds([]);
  }

  function toggleSelected(transactionId: string) {
    setSelectedIds((current) =>
      current.includes(transactionId)
        ? current.filter((id) => id !== transactionId)
        : [...current, transactionId]
    );
  }

  return (
    <section className="grid gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Transactions</h2>
          <p className="text-sm text-muted-foreground">
            Filter, search, edit categories, and prepare bulk cleanup.
          </p>
        </div>
        <Button
          variant="ghost"
          onClick={() => {
            setSearch("");
            setAccountId("");
            setCategoryId("");
            setDirection("");
            setStatus("");
            setFrom("");
            setTo("");
            setSort("-occurredAt");
            setPage(0);
            setSelectedIds([]);
          }}
        >
          Clear filters
        </Button>
      </div>

      <div className="grid gap-2 rounded-lg border border-border bg-card p-3 shadow-raised md:grid-cols-[1.4fr_repeat(4,1fr)]">
        <label className="relative block">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="h-10 pl-9"
            value={search}
            placeholder="Search merchant, account, note, reference"
            onChange={(event) => resetPage(() => setSearch(event.target.value))}
          />
        </label>
        <FilterSelect value={accountId} onChange={(value) => resetPage(() => setAccountId(value))}>
          <option value="">All accounts</option>
          {(accountsQuery.data ?? []).map((account) => (
            <option key={account.id} value={account.id}>
              {account.displayName}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          value={categoryId}
          onChange={(value) => resetPage(() => setCategoryId(value))}
        >
          <option value="">All categories</option>
          {(categoriesQuery.data ?? []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          value={direction}
          onChange={(value) => resetPage(() => setDirection(value as TransactionDirection | ""))}
        >
          <option value="">Money in/out</option>
          <option value="DEBIT">Debits</option>
          <option value="CREDIT">Credits</option>
        </FilterSelect>
        <FilterSelect value={sort} onChange={(value) => resetPage(() => setSort(value))}>
          <option value="-occurredAt">Newest first</option>
          <option value="occurredAt">Oldest first</option>
          <option value="-amount">Highest amount</option>
          <option value="merchant">Merchant A-Z</option>
        </FilterSelect>
        <Input
          className="h-10"
          type="date"
          value={from}
          onChange={(event) => resetPage(() => setFrom(event.target.value))}
        />
        <Input
          className="h-10"
          type="date"
          value={to}
          onChange={(event) => resetPage(() => setTo(event.target.value))}
        />
        <FilterSelect
          value={status}
          onChange={(value) => resetPage(() => setStatus(value as TransactionStatus | ""))}
        >
          <option value="">All statuses</option>
          <option value="POSTED">Posted</option>
          <option value="PENDING">Pending</option>
          <option value="EXCLUDED">Excluded</option>
        </FilterSelect>
      </div>

      <div className="grid gap-2 rounded-lg border border-border bg-card p-3 shadow-raised md:grid-cols-[auto_1fr_1fr_auto] md:items-center">
        <p className="text-sm font-medium">{selectedIds.length} selected</p>
        <FilterSelect value={bulkCategoryId} onChange={setBulkCategoryId}>
          <option value="">Bulk category</option>
          {(categoriesQuery.data ?? []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect
          value={bulkStatus}
          onChange={(value) => setBulkStatus(value as TransactionStatus | "")}
        >
          <option value="">Bulk status</option>
          <option value="POSTED">Posted</option>
          <option value="EXCLUDED">Excluded</option>
        </FilterSelect>
        <Button
          disabled={
            selectedIds.length === 0 || bulkUpdate.isPending || (!bulkCategoryId && !bulkStatus)
          }
          onClick={() =>
            bulkUpdate.mutate(
              {
                transactionIds: selectedIds,
                categoryId: bulkCategoryId || undefined,
                status: bulkStatus || undefined,
                reason: "Manual bulk action",
              },
              {
                onSuccess: () => {
                  setSelectedIds([]);
                  setBulkCategoryId("");
                  setBulkStatus("");
                },
              }
            )
          }
        >
          {bulkUpdate.isPending ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
          Apply
        </Button>
      </div>

      {transactionsQuery.isLoading ? (
        <div className="grid gap-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : (
        <TransactionList
          transactions={transactionPage?.items ?? []}
          selectedIds={selectedIds}
          onToggleSelected={toggleSelected}
        />
      )}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-muted-foreground">
          {transactionPage
            ? `${transactionPage.totalItems} transaction(s), page ${transactionPage.page + 1} of ${Math.max(
                transactionPage.totalPages,
                1
              )}`
            : "Transactions loading"}
        </p>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={!transactionPage?.hasPrevious}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            <ArrowLeft className="size-4" aria-hidden />
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={!transactionPage?.hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
            <ArrowRight className="size-4" aria-hidden />
          </Button>
        </div>
      </div>
    </section>
  );
}

function PlanningWorkspace({ summary }: { summary: DashboardFinanceSummary }) {
  const [budgetDialog, setBudgetDialog] = useState<{ open: boolean; budget?: Budget | null }>({
    open: false,
  });
  const [goalDialog, setGoalDialog] = useState<{ open: boolean; goal?: SavingsGoal | null }>({
    open: false,
  });
  const [contributionGoal, setContributionGoal] = useState<SavingsGoal | null>(null);
  const budgetHistoryQuery = useBudgetHistory();
  const deleteBudget = useDeleteBudget();
  const deleteGoal = useDeleteSavingsGoal();
  const budgetOverview = summary.budgetOverview ?? emptyBudgetOverview;
  const budgets = budgetOverview.budgets ?? [];
  const goals = summary.savingsGoals ?? [];
  const topOverspendingCategories = summary.topOverspendingCategories ?? [];

  return (
    <section className="grid gap-4">
      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <CardTitle className="text-base">Budget overview</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">
                Monthly category limits calculated against posted spending.
              </p>
            </div>
            <Button size="sm" onClick={() => setBudgetDialog({ open: true, budget: null })}>
              <Plus className="size-4" aria-hidden />
              Budget
            </Button>
          </CardHeader>
          <CardContent className="grid gap-4">
            {budgets.length === 0 ? (
              <PlanningEmpty
                icon={WalletCards}
                title="Create the first monthly budget"
                body="Pick one real spending category and set a gentle limit for this month."
                action="Add budget"
                onAction={() => setBudgetDialog({ open: true, budget: null })}
              />
            ) : (
              <>
                <div className="grid gap-3 sm:grid-cols-3">
                  <MiniMetric
                    label="Budgeted"
                    value={formatMoney(budgetOverview.totalBudgeted)}
                  />
                  <MiniMetric
                    label="Remaining"
                    value={formatMoney(budgetOverview.totalRemaining)}
                  />
                  <MiniMetric
                    label="Pressure"
                    value={`${Math.round(budgetOverview.usagePercent)}%`}
                    state={budgetOverview.state}
                  />
                </div>
                <div className="grid gap-3">
                  {budgets.slice(0, 4).map((budget) => (
                    <BudgetProgressCard
                      key={budget.id}
                      budget={budget}
                      onEdit={() => setBudgetDialog({ open: true, budget })}
                      onDelete={() => deleteBudget.mutate(budget.id)}
                    />
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <CardTitle className="text-base">Savings goals</CardTitle>
              <p className="mt-1 text-sm text-muted-foreground">
                Progress comes from explicit contributions, not inferred transfers.
              </p>
            </div>
            <Button size="sm" onClick={() => setGoalDialog({ open: true, goal: null })}>
              <Target className="size-4" aria-hidden />
              Goal
            </Button>
          </CardHeader>
          <CardContent className="grid gap-4">
            {goals.length === 0 ? (
              <PlanningEmpty
                icon={PiggyBank}
                title="Start a calm savings goal"
                body="Set a target and add contributions when money is actually moved."
                action="Add goal"
                onAction={() => setGoalDialog({ open: true, goal: null })}
              />
            ) : (
              <div className="grid gap-3">
                {goals.slice(0, 3).map((goal) => (
                  <GoalProgressCard
                    key={goal.id}
                    goal={goal}
                    onContribute={() => setContributionGoal(goal)}
                    onEdit={() => setGoalDialog({ open: true, goal })}
                    onDelete={() => deleteGoal.mutate(goal.id)}
                  />
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <FinancialHealthCard summary={summary} />
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Category trends</CardTitle>
          </CardHeader>
          <CardContent>
            <CategoryTrendWidget data={summary.categoryTrends} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Top budget pressure</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {topOverspendingCategories.length === 0 ? (
              <ChartEmpty label="No category is over budget right now" />
            ) : (
              topOverspendingCategories.map((item) => (
                <WidgetLine
                  key={item.categoryId ?? item.name}
                  label={item.name}
                  value={`${formatMoney(item.total)} over`}
                />
              ))
            )}
          </CardContent>
        </Card>
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Budget history</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            {(budgetHistoryQuery.data ?? []).length === 0 ? (
              <ChartEmpty label="Budget changes will appear here" />
            ) : (
              (budgetHistoryQuery.data ?? []).slice(0, 5).map((entry) => (
                <div
                  key={entry.id}
                  className="rounded-lg border border-border/70 bg-background/60 px-3 py-2 text-sm"
                >
                  <div className="flex items-center justify-between gap-3">
                    <span className="font-medium">{entry.action.toLowerCase()}</span>
                    <span className="text-xs text-muted-foreground">
                      {new Date(entry.createdAt).toLocaleDateString("en-IN")}
                    </span>
                  </div>
                  <p className="mt-1 text-muted-foreground">
                    {entry.newName ?? entry.budgetName ?? "Budget"}{" "}
                    {entry.newAmount ? `at ${formatMoney(entry.newAmount)}` : ""}
                  </p>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </section>

      {budgetDialog.open ? (
        <BudgetDialog
          budget={budgetDialog.budget}
          open={budgetDialog.open}
          onOpenChange={(open) =>
            setBudgetDialog({ open, budget: open ? budgetDialog.budget : null })
          }
        />
      ) : null}
      {goalDialog.open ? (
        <GoalDialog
          goal={goalDialog.goal}
          open={goalDialog.open}
          onOpenChange={(open) => setGoalDialog({ open, goal: open ? goalDialog.goal : null })}
        />
      ) : null}
      {contributionGoal ? (
        <ContributionDialog
          goal={contributionGoal}
          onOpenChange={(open) => !open && setContributionGoal(null)}
        />
      ) : null}
    </section>
  );
}

function BudgetProgressCard({
  budget,
  onEdit,
  onDelete,
}: {
  budget: Budget;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="grid gap-3 rounded-lg border border-border bg-background/70 p-3 transition-colors hover:bg-muted/25">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <StateDot state={budget.state} />
            <h3 className="truncate text-sm font-semibold">{budget.name}</h3>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">{budget.category.name}</p>
        </div>
        <div className="flex gap-1">
          <Button size="icon-xs" variant="ghost" onClick={onEdit} title="Edit budget">
            <Edit3 className="size-3" aria-hidden />
          </Button>
          <Button size="icon-xs" variant="ghost" onClick={onDelete} title="End budget">
            <Trash2 className="size-3" aria-hidden />
          </Button>
        </div>
      </div>
      <ProgressBar value={budget.usagePercent} state={budget.state} />
      <div className="grid grid-cols-3 gap-2 text-xs">
        <MiniMetric label="Spent" value={formatMoney(budget.spent)} />
        <MiniMetric label="Limit" value={formatMoney(budget.amount)} />
        <MiniMetric
          label={budget.remaining >= 0 ? "Left" : "Over"}
          value={formatMoney(Math.abs(budget.remaining))}
          state={budget.state}
        />
      </div>
    </div>
  );
}

function GoalProgressCard({
  goal,
  onContribute,
  onEdit,
  onDelete,
}: {
  goal: SavingsGoal;
  onContribute: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="grid gap-3 rounded-lg border border-border bg-background/70 p-3 transition-colors hover:bg-muted/25">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            {goal.status === "COMPLETED" ? (
              <CheckCircle2 className="size-4 text-success" aria-hidden />
            ) : (
              <Target className="size-4 text-primary" aria-hidden />
            )}
            <h3 className="truncate text-sm font-semibold">{goal.name}</h3>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {goal.targetDate
              ? `Target ${new Date(goal.targetDate).toLocaleDateString("en-IN")}`
              : "Open timeline"}
          </p>
        </div>
        <div className="flex gap-1">
          <Button size="icon-xs" variant="ghost" onClick={onContribute} title="Add contribution">
            <Plus className="size-3" aria-hidden />
          </Button>
          <Button size="icon-xs" variant="ghost" onClick={onEdit} title="Edit goal">
            <Edit3 className="size-3" aria-hidden />
          </Button>
          <Button size="icon-xs" variant="ghost" onClick={onDelete} title="Remove goal">
            <Trash2 className="size-3" aria-hidden />
          </Button>
        </div>
      </div>
      <GoalDonut value={goal.progressPercent} />
      <div className="grid grid-cols-3 gap-2 text-xs">
        <MiniMetric label="Saved" value={formatMoney(goal.currentAmount)} />
        <MiniMetric label="Target" value={formatMoney(goal.targetAmount)} />
        <MiniMetric
          label="Monthly"
          value={goal.monthlyTarget > 0 ? formatMoney(goal.monthlyTarget) : "Flexible"}
        />
      </div>
    </div>
  );
}

function FinancialHealthCard({ summary }: { summary: DashboardFinanceSummary }) {
  const health = summary.financialHealth ?? emptyFinancialHealth;
  const savingsMomentum = summary.savingsMomentum ?? emptySavingsMomentum;
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Financial health</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="flex items-center justify-between gap-4 rounded-lg border border-border/70 bg-background/70 p-4">
          <div>
            <div className="flex items-center gap-2">
              <StateDot state={health.state} />
              <p className="text-sm font-medium">{stateLabel(health.state)}</p>
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              A deterministic foundation from cashflow, consistency, and budget pressure.
            </p>
          </div>
          <div className="text-right">
            <p className="text-3xl font-semibold tabular-nums">{health.score}</p>
            <p className="text-xs text-muted-foreground">score</p>
          </div>
        </div>
        <div className="grid gap-2 sm:grid-cols-2">
          <MiniMetric
            label="Savings ratio"
            value={`${Math.round(health.savingsRatio)}%`}
            state={savingsMomentum.state}
          />
          <MiniMetric label="Consistency" value={`${Math.round(health.spendingConsistency)}%`} />
          <MiniMetric label="Stability" value={`${Math.round(health.incomeExpenseStability)}%`} />
          <MiniMetric
            label="Goal contributions"
            value={formatMoney(savingsMomentum.goalContributionsThisMonth)}
          />
        </div>
      </CardContent>
    </Card>
  );
}

function CategoryTrendWidget({ data }: { data: CategoryTrend[] }) {
  if (data.length === 0) {
    return <ChartEmpty label="Category trend appears after monthly spending data builds up" />;
  }
  const grouped = Object.values(
    data.reduce<Record<string, { name: string; colorToken: string; total: number }>>(
      (acc, item) => {
        const key = item.categoryId ?? item.name;
        acc[key] = acc[key] ?? { name: item.name, colorToken: item.colorToken, total: 0 };
        acc[key].total += item.total;
        return acc;
      },
      {}
    )
  )
    .sort((a, b) => b.total - a.total)
    .slice(0, 5);
  const max = Math.max(...grouped.map((item) => item.total), 1);
  return (
    <div className="grid gap-3">
      {grouped.map((item, index) => (
        <div key={item.name} className="grid grid-cols-[7rem_1fr_auto] items-center gap-3 text-sm">
          <span className="truncate text-muted-foreground">{item.name}</span>
          <div className="h-2 rounded-full bg-muted">
            <div
              className="h-full rounded-full transition-[width] duration-500"
              style={{
                width: `${(item.total / max) * 100}%`,
                background: `var(--chart-${(index % 5) + 1})`,
              }}
            />
          </div>
          <span className="text-xs tabular-nums">{formatMoney(item.total)}</span>
        </div>
      ))}
    </div>
  );
}

function BudgetDialog({
  open,
  budget,
  onOpenChange,
}: {
  open: boolean;
  budget?: Budget | null;
  onOpenChange: (open: boolean) => void;
}) {
  const categoriesQuery = useCategories();
  const createBudget = useCreateBudget();
  const updateBudget = useUpdateBudget(budget?.id);
  const [categoryId, setCategoryId] = useState(budget?.category.id ?? "");
  const [name, setName] = useState(budget?.name ?? "");
  const [amount, setAmount] = useState(budget?.amount ? String(budget.amount) : "");
  const [startsOn, setStartsOn] = useState(
    (budget?.periodStart ?? new Date().toISOString()).slice(0, 10)
  );
  const [rolloverEnabled, setRolloverEnabled] = useState(Boolean(budget?.rolloverEnabled));

  function submit(event: FormEvent) {
    event.preventDefault();
    const payload = {
      categoryId,
      name,
      amount: Number(amount),
      currency: "INR",
      startsOn,
      rolloverEnabled,
      reason: budget ? "Budget edited from dashboard" : "Budget created from dashboard",
    };
    const mutation = budget ? updateBudget : createBudget;
    mutation.mutate(payload, { onSuccess: () => onOpenChange(false) });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{budget ? "Edit budget" : "Create budget"}</DialogTitle>
          <DialogDescription>
            Set one monthly category limit using your existing imported categories.
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={submit}>
          <FilterSelect value={categoryId} onChange={setCategoryId}>
            <option value="">Choose category</option>
            {(categoriesQuery.data ?? []).map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </FilterSelect>
          <Input
            required
            value={name}
            placeholder="Budget name"
            onChange={(event) => setName(event.target.value)}
          />
          <Input
            required
            min="1"
            type="number"
            value={amount}
            placeholder="Monthly amount"
            onChange={(event) => setAmount(event.target.value)}
          />
          <Input
            type="date"
            value={startsOn}
            onChange={(event) => setStartsOn(event.target.value)}
          />
          <label className="flex items-center gap-2 text-sm text-muted-foreground">
            <input
              checked={rolloverEnabled}
              type="checkbox"
              onChange={(event) => setRolloverEnabled(event.target.checked)}
            />
            Prepare rollover tracking
          </label>
          <DialogFooter>
            <Button disabled={!categoryId || createBudget.isPending || updateBudget.isPending}>
              {createBudget.isPending || updateBudget.isPending ? (
                <Loader2 className="size-4 animate-spin" aria-hidden />
              ) : null}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function GoalDialog({
  open,
  goal,
  onOpenChange,
}: {
  open: boolean;
  goal?: SavingsGoal | null;
  onOpenChange: (open: boolean) => void;
}) {
  const createGoal = useCreateSavingsGoal();
  const updateGoal = useUpdateSavingsGoal(goal?.id);
  const [name, setName] = useState(goal?.name ?? "");
  const [targetAmount, setTargetAmount] = useState(
    goal?.targetAmount ? String(goal.targetAmount) : ""
  );
  const [currentAmount, setCurrentAmount] = useState(
    goal?.currentAmount ? String(goal.currentAmount) : ""
  );
  const [targetDate, setTargetDate] = useState(goal?.targetDate?.slice(0, 10) ?? "");

  function submit(event: FormEvent) {
    event.preventDefault();
    const payload = {
      name,
      targetAmount: Number(targetAmount),
      currentAmount: goal ? undefined : Number(currentAmount || 0),
      currency: "INR",
      targetDate: targetDate || undefined,
      colorToken: "green",
      iconName: "target",
    };
    const mutation = goal ? updateGoal : createGoal;
    mutation.mutate(payload, { onSuccess: () => onOpenChange(false) });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{goal ? "Edit goal" : "Create savings goal"}</DialogTitle>
          <DialogDescription>
            Goal progress updates only when a contribution is recorded.
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={submit}>
          <Input
            required
            value={name}
            placeholder="Goal name"
            onChange={(event) => setName(event.target.value)}
          />
          <Input
            required
            min="1"
            type="number"
            value={targetAmount}
            placeholder="Target amount"
            onChange={(event) => setTargetAmount(event.target.value)}
          />
          {!goal ? (
            <Input
              min="0"
              type="number"
              value={currentAmount}
              placeholder="Already saved"
              onChange={(event) => setCurrentAmount(event.target.value)}
            />
          ) : null}
          <Input
            type="date"
            value={targetDate}
            onChange={(event) => setTargetDate(event.target.value)}
          />
          <DialogFooter>
            <Button disabled={createGoal.isPending || updateGoal.isPending}>
              {createGoal.isPending || updateGoal.isPending ? (
                <Loader2 className="size-4 animate-spin" aria-hidden />
              ) : null}
              Save
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ContributionDialog({
  goal,
  onOpenChange,
}: {
  goal: SavingsGoal | null;
  onOpenChange: (open: boolean) => void;
}) {
  const addContribution = useAddGoalContribution(goal?.id);
  const [amount, setAmount] = useState("");
  const [contributedOn, setContributedOn] = useState(new Date().toISOString().slice(0, 10));
  const [note, setNote] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    addContribution.mutate(
      { amount: Number(amount), contributedOn, note: note || undefined },
      {
        onSuccess: () => {
          setAmount("");
          setNote("");
          onOpenChange(false);
        },
      }
    );
  }

  return (
    <Dialog open={Boolean(goal)} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Add contribution</DialogTitle>
          <DialogDescription>
            {goal?.name ?? "Goal"} will update from this recorded amount.
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={submit}>
          <Input
            required
            min="1"
            type="number"
            value={amount}
            placeholder="Amount"
            onChange={(event) => setAmount(event.target.value)}
          />
          <Input
            type="date"
            value={contributedOn}
            onChange={(event) => setContributedOn(event.target.value)}
          />
          <Input
            value={note}
            placeholder="Note"
            onChange={(event) => setNote(event.target.value)}
          />
          <DialogFooter>
            <Button disabled={addContribution.isPending}>
              {addContribution.isPending ? (
                <Loader2 className="size-4 animate-spin" aria-hidden />
              ) : null}
              Add
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function CategoryManagementPanel() {
  const categoriesQuery = useCategories();
  const categories = categoriesQuery.data ?? [];
  const customCategories = categories.filter((category) => !category.systemCategory);
  const [editingCategoryId, setEditingCategoryId] = useState<string | null>(null);
  const [mergeCategoryId, setMergeCategoryId] = useState<string | null>(null);

  return (
    <section className="grid gap-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Categories</h2>
          <p className="text-sm text-muted-foreground">
            Create custom categories, tune labels, and merge cleanup categories.
          </p>
        </div>
        <Button variant="outline" onClick={() => setEditingCategoryId("new")}>
          <Plus className="size-4" aria-hidden />
          Category
        </Button>
      </div>
      <div className="grid gap-2 rounded-lg border border-border bg-card p-3 shadow-raised md:grid-cols-2 xl:grid-cols-4">
        {categories.map((category) => (
          <div
            key={category.id}
            className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/70 px-3 py-2"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span className="size-2.5 rounded-full bg-primary" />
                <span className="truncate text-sm font-medium">{category.name}</span>
              </div>
              <p className="text-xs text-muted-foreground">
                {category.systemCategory
                  ? "System"
                  : `${category.iconName} / ${category.colorToken}`}
              </p>
            </div>
            {!category.systemCategory ? (
              <div className="flex gap-1">
                <Button
                  size="icon-xs"
                  variant="ghost"
                  onClick={() => setEditingCategoryId(category.id)}
                  title="Rename category"
                >
                  <Edit3 className="size-3" aria-hidden />
                </Button>
                <Button
                  size="icon-xs"
                  variant="ghost"
                  onClick={() => setMergeCategoryId(category.id)}
                  title="Merge category"
                >
                  <Merge className="size-3" aria-hidden />
                </Button>
              </div>
            ) : null}
          </div>
        ))}
      </div>
      {editingCategoryId ? (
        <CategoryDialog
          category={
            editingCategoryId === "new"
              ? null
              : categories.find((category) => category.id === editingCategoryId)
          }
          open={Boolean(editingCategoryId)}
          onOpenChange={(open) => !open && setEditingCategoryId(null)}
        />
      ) : null}
      {mergeCategoryId ? (
        <CategoryMergeDialog
          category={customCategories.find((category) => category.id === mergeCategoryId) ?? null}
          categories={categories}
          open={Boolean(mergeCategoryId)}
          onOpenChange={(open) => !open && setMergeCategoryId(null)}
        />
      ) : null}
    </section>
  );
}

function CategoryDialog({
  category,
  open,
  onOpenChange,
}: {
  category?: { id: string; name: string; colorToken: string; iconName: string } | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory(category?.id);
  const [name, setName] = useState(category?.name ?? "");
  const [colorToken, setColorToken] = useState(category?.colorToken ?? "green");
  const [iconName, setIconName] = useState(category?.iconName ?? "tag");

  function submit(event: FormEvent) {
    event.preventDefault();
    const payload = {
      name,
      colorToken,
      iconName,
      reason: category
        ? "Category updated from dashboard"
        : "Custom category created from dashboard",
    };
    const mutation = category ? updateCategory : createCategory;
    mutation.mutate(payload, { onSuccess: () => onOpenChange(false) });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{category ? "Rename category" : "Create category"}</DialogTitle>
          <DialogDescription>
            Custom categories remain auditable and available for budgets.
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={submit}>
          <Input
            required
            value={name}
            placeholder="Category name"
            onChange={(event) => setName(event.target.value)}
          />
          <FilterSelect value={colorToken} onChange={setColorToken}>
            <option value="green">Green</option>
            <option value="blue">Blue</option>
            <option value="amber">Amber</option>
            <option value="teal">Teal</option>
            <option value="neutral">Neutral</option>
          </FilterSelect>
          <FilterSelect value={iconName} onChange={setIconName}>
            <option value="tag">Tag</option>
            <option value="wallet">Wallet</option>
            <option value="receipt">Receipt</option>
            <option value="target">Target</option>
            <option value="heart-pulse">Health</option>
          </FilterSelect>
          <DialogFooter>
            <Button disabled={createCategory.isPending || updateCategory.isPending}>Save</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function CategoryMergeDialog({
  category,
  categories,
  open,
  onOpenChange,
}: {
  category: { id: string; name: string } | null;
  categories: { id: string; name: string }[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const mergeCategory = useMergeCategory(category?.id);
  const [targetCategoryId, setTargetCategoryId] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    mergeCategory.mutate(
      { targetCategoryId, reason: "Category merged from dashboard" },
      { onSuccess: () => onOpenChange(false) }
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Merge category</DialogTitle>
          <DialogDescription>
            Transactions and budgets using {category?.name ?? "this category"} move to the target
            category.
          </DialogDescription>
        </DialogHeader>
        <form className="grid gap-3" onSubmit={submit}>
          <FilterSelect value={targetCategoryId} onChange={setTargetCategoryId}>
            <option value="">Choose target</option>
            {categories
              .filter((item) => item.id !== category?.id)
              .map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
          </FilterSelect>
          <DialogFooter>
            <Button disabled={!targetCategoryId || mergeCategory.isPending}>
              {mergeCategory.isPending ? (
                <Loader2 className="size-4 animate-spin" aria-hidden />
              ) : null}
              Merge
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function PlanningEmpty({
  icon: Icon,
  title,
  body,
  action,
  onAction,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  title: string;
  body: string;
  action: string;
  onAction: () => void;
}) {
  return (
    <div className="grid gap-3 rounded-lg border border-dashed border-border bg-muted/20 p-5 text-center">
      <Icon className="mx-auto size-7 text-primary" aria-hidden />
      <div>
        <h3 className="text-sm font-semibold">{title}</h3>
        <p className="mt-1 text-sm text-muted-foreground">{body}</p>
      </div>
      <Button className="mx-auto" size="sm" variant="outline" onClick={onAction}>
        <Plus className="size-4" aria-hidden />
        {action}
      </Button>
    </div>
  );
}

function MiniMetric({
  label,
  value,
  state,
}: {
  label: string;
  value: string;
  state?: "HEALTHY" | "CAUTION" | "RISK";
}) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/65 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn("mt-1 truncate text-sm font-semibold tabular-nums", stateTextClass(state))}>
        {value}
      </p>
    </div>
  );
}

function ProgressBar({ value, state }: { value: number; state: "HEALTHY" | "CAUTION" | "RISK" }) {
  return (
    <div className="h-2 overflow-hidden rounded-full bg-muted">
      <div
        className={cn("h-full rounded-full transition-[width] duration-500", stateFillClass(state))}
        style={{ width: `${Math.min(Math.max(value, 3), 100)}%` }}
      />
    </div>
  );
}

function GoalDonut({ value }: { value: number }) {
  const normalized = Math.min(Math.max(value, 0), 100);
  return (
    <div className="flex items-center gap-3">
      <svg className="size-14 shrink-0" viewBox="0 0 44 44" role="img" aria-label="Goal progress">
        <circle cx="22" cy="22" r="17" fill="none" stroke="var(--muted)" strokeWidth="6" />
        <circle
          cx="22"
          cy="22"
          r="17"
          fill="none"
          stroke="var(--success)"
          strokeDasharray={`${normalized} ${100 - normalized}`}
          strokeLinecap="round"
          strokeWidth="6"
          transform="rotate(-90 22 22)"
          pathLength="100"
        />
      </svg>
      <div>
        <p className="text-sm font-semibold tabular-nums">{Math.round(normalized)}%</p>
        <p className="text-xs text-muted-foreground">complete</p>
      </div>
    </div>
  );
}

function StateDot({ state }: { state: "HEALTHY" | "CAUTION" | "RISK" }) {
  return <span className={cn("size-2.5 rounded-full", stateFillClass(state))} aria-hidden />;
}

function stateLabel(state: "HEALTHY" | "CAUTION" | "RISK") {
  if (state === "HEALTHY") {
    return "Healthy";
  }
  if (state === "CAUTION") {
    return "Caution";
  }
  return "Needs attention";
}

function stateLabelForInsight(state: string) {
  if (state === "HEALTHY") {
    return "steady";
  }
  if (state === "CAUTION") {
    return "watch";
  }
  if (state === "RISK") {
    return "review";
  }
  return "waiting";
}

function stateFillClass(state?: "HEALTHY" | "CAUTION" | "RISK") {
  if (state === "RISK") {
    return "bg-info";
  }
  if (state === "CAUTION") {
    return "bg-warning";
  }
  return "bg-success";
}

function stateTextClass(state?: "HEALTHY" | "CAUTION" | "RISK") {
  if (state === "RISK") {
    return "text-info";
  }
  if (state === "CAUTION") {
    return "text-warning";
  }
  if (state === "HEALTHY") {
    return "text-success";
  }
  return "";
}

function TrendLineChart({ data }: { data: MonthlySummary[] }) {
  const chart = useMemo(() => {
    const values = data.map((item) => item.expense);
    const max = Math.max(...values, 1);
    const points = data.map((item, index) => {
      const x = data.length === 1 ? 50 : (index / (data.length - 1)) * 100;
      const y = 90 - (item.expense / max) * 75;
      return `${x},${y}`;
    });
    return { points };
  }, [data]);

  if (data.length === 0) {
    return <ChartEmpty label="No trend yet" />;
  }
  return (
    <div className="grid gap-3">
      <svg
        className="h-48 w-full overflow-visible"
        role="img"
        aria-label="Monthly spending trend"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
      >
        <polyline
          fill="none"
          points={chart.points.join(" ")}
          stroke="var(--primary)"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="2.5"
          vectorEffect="non-scaling-stroke"
        />
        {chart.points.map((point) => {
          const [x, y] = point.split(",");
          return (
            <circle
              key={point}
              cx={x}
              cy={y}
              fill="var(--background)"
              r="1.8"
              stroke="var(--primary)"
              strokeWidth="1.5"
              vectorEffect="non-scaling-stroke"
            />
          );
        })}
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground sm:grid-cols-6">
        {data.map((item) => (
          <span key={item.periodStart}>
            {new Date(item.periodStart).toLocaleDateString("en-IN", { month: "short" })}
          </span>
        ))}
      </div>
      <table className="sr-only">
        <caption>Monthly spending trend values</caption>
        <tbody>
          {data.map((item) => (
            <tr key={`${item.periodStart}-a11y`}>
              <th scope="row">
                {new Date(item.periodStart).toLocaleDateString("en-IN", {
                  month: "long",
                  year: "numeric",
                })}
              </th>
              <td>{formatMoney(item.expense)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CategoryDistribution({ data }: { data: CategorySpend[] }) {
  const visibleData = useMemo(() => data.slice(0, 6), [data]);
  if (data.length === 0) {
    return <ChartEmpty label="No category spending this month" />;
  }
  return (
    <div className="grid gap-3">
      {visibleData.map((item) => (
        <div key={item.categoryId ?? item.name} className="grid gap-1">
          <div className="flex items-center justify-between gap-3 text-sm">
            <span className="truncate font-medium">{item.name}</span>
            <span className="tabular-nums text-muted-foreground">{formatMoney(item.total)}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary transition-[width] duration-500"
              style={{ width: `${Math.max(3, item.share)}%` }}
              aria-hidden
            />
          </div>
        </div>
      ))}
      <table className="sr-only">
        <caption>Category spending values</caption>
        <tbody>
          {visibleData.map((item) => (
            <tr key={`${item.categoryId ?? item.name}-a11y`}>
              <th scope="row">{item.name}</th>
              <td>{formatMoney(item.total)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MonthlyComparison({ data }: { data: MonthlySummary[] }) {
  if (data.length === 0) {
    return <ChartEmpty label="No monthly comparison yet" />;
  }
  const max = Math.max(...data.flatMap((item) => [item.income, item.expense]), 1);
  return (
    <div className="grid gap-3">
      {data.map((item) => (
        <div
          key={item.periodStart}
          className="grid grid-cols-[3.5rem_1fr] items-center gap-3 text-sm"
        >
          <span className="text-xs text-muted-foreground">
            {new Date(item.periodStart).toLocaleDateString("en-IN", { month: "short" })}
          </span>
          <div className="grid gap-1">
            <div className="h-2 rounded-full bg-success/20">
              <div
                className="h-full rounded-full bg-success"
                style={{ width: `${(item.income / max) * 100}%` }}
              />
            </div>
            <div className="h-2 rounded-full bg-primary/15">
              <div
                className="h-full rounded-full bg-primary"
                style={{ width: `${(item.expense / max) * 100}%` }}
              />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function ChartEmpty({ label }: { label: string }) {
  return (
    <div className="grid min-h-44 place-items-center rounded-lg border border-dashed border-border bg-muted/20 p-5 text-center text-sm text-muted-foreground">
      {label}
    </div>
  );
}

function DashboardEmptyState({ accountCount }: { accountCount: number }) {
  const seedDemo = useSeedDemoFinanceData();

  return (
    <main className="grid gap-5">
      <section className="grid gap-4 rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6 lg:grid-cols-[1.3fr_0.7fr]">
        <div className="space-y-4">
          <div className="inline-flex w-fit items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden />
            Data foundation ready
          </div>
          <div className="space-y-2">
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
              No transactions yet.
            </h2>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground sm:text-base">
              Import a CSV to activate real dashboard totals, category charts, and account activity.
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button render={<Link href="/accounts" />}>
              <LinkIcon className="size-4" aria-hidden />
              Accounts
            </Button>
            <Button variant="outline" render={<Link href="/imports" />}>
              <FileUp className="size-4" aria-hidden />
              Import CSV
            </Button>
            <Button
              variant="outline"
              disabled={seedDemo.isPending}
              onClick={() => seedDemo.mutate()}
            >
              {seedDemo.isPending ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
              Seed local demo
            </Button>
          </div>
        </div>
        <Card className="rounded-lg border-border bg-muted/25 shadow-none">
          <CardHeader>
            <CardTitle className="text-sm font-medium">Workspace state</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 text-sm text-muted-foreground">
            <WidgetLine label="Profile" value="Complete" />
            <WidgetLine
              label="Accounts"
              value={accountCount > 0 ? `${accountCount} connected` : "Waiting"}
            />
            <WidgetLine label="Transactions" value="Empty" />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <FinancialCard
          title="Account balance"
          value={formatMoney(0)}
          detail="No account balances yet."
          icon={Landmark}
        />
        <FinancialCard
          title="Spending overview"
          value="No data"
          detail="Charts appear after imports."
          icon={ReceiptText}
        />
        <FinancialCard
          title="Net cashflow"
          value={formatMoney(0)}
          detail="No cashflow yet."
          icon={CircleDollarSign}
        />
      </section>

      <TransactionList transactions={[]} emptyLabel="Your first transaction will appear here." />
    </main>
  );
}

function DashboardLoadingState() {
  return (
    <main className="grid gap-5">
      <section className="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
        <div className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
          <div className="space-y-2">
            <p className="text-sm font-medium text-muted-foreground">Preparing ledger view</p>
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
              Your money view is based on imported transactions.
            </h2>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
              Spending, income, balances, categories, and imports are loading from your workspace.
            </p>
          </div>
        </div>
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Recent import activity
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      </section>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <FinancialCard
          title="Account balance"
          value="Loading"
          detail="Account totals are syncing."
          icon={Landmark}
        />
        <FinancialCard
          title="This month spent"
          value="Loading"
          detail="Posted debits are syncing."
          icon={ReceiptText}
        />
        <FinancialCard
          title="This month received"
          value="Loading"
          detail="Posted credits are syncing."
          icon={CircleDollarSign}
        />
        <FinancialCard
          title="Net cashflow"
          value="Loading"
          detail="Monthly cashflow is syncing."
          icon={TrendingUp}
        />
      </div>
      <DashboardDeferredPlaceholder />
    </main>
  );
}

function DashboardDeferredPlaceholder() {
  return (
    <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]" aria-hidden="true">
      <Skeleton className="h-48 w-full" />
      <Skeleton className="h-48 w-full" />
    </section>
  );
}

function FilterSelect({
  children,
  className,
  value,
  onChange,
}: {
  children: ReactNode;
  className?: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <select
      className={cn(
        "h-10 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50",
        className
      )}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      {children}
    </select>
  );
}

function WidgetLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/60 px-3 py-2">
      <span className="truncate text-muted-foreground">{label}</span>
      <span className="shrink-0 font-medium tabular-nums">{value}</span>
    </div>
  );
}
