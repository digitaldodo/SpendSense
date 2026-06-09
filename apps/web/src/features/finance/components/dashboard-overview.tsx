"use client";

import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import {
  ArrowLeft,
  ArrowRight,
  CircleDollarSign,
  FileUp,
  Landmark,
  LinkIcon,
  Loader2,
  ReceiptText,
  Search,
  ShieldCheck,
  TrendingUp,
} from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { FinancialCard } from "@/features/finance/components/financial-card";
import { TransactionList } from "@/features/finance/components/transaction-list";
import {
  useAccounts,
  useBulkUpdateTransactions,
  useCategories,
  useDashboardFinanceSummary,
  useSeedDemoFinanceData,
  useTransactions,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type {
  CategorySpend,
  MonthlySummary,
  TransactionDirection,
  TransactionStatus,
} from "@/features/finance/types";
import { cn } from "@/lib/utils";

const pageSize = 12;

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
              Import
            </Button>
          </div>
        </div>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">Recent import activity</CardTitle>
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
                    {job.recordsImported} imported, {job.recordsDuplicate} skipped
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
          detail={summary.netCashflow >= 0 ? "Income is ahead this month." : "Expenses are ahead this month."}
          icon={TrendingUp}
        />
      </section>

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
            <CardTitle className="text-base">Account balances</CardTitle>
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

      <TransactionExplorer />
    </main>
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
          <p className="text-sm text-muted-foreground">Filter, search, edit categories, and prepare bulk cleanup.</p>
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
        <FilterSelect value={categoryId} onChange={(value) => resetPage(() => setCategoryId(value))}>
          <option value="">All categories</option>
          {(categoriesQuery.data ?? []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect value={direction} onChange={(value) => resetPage(() => setDirection(value as TransactionDirection | ""))}>
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
        <Input className="h-10" type="date" value={from} onChange={(event) => resetPage(() => setFrom(event.target.value))} />
        <Input className="h-10" type="date" value={to} onChange={(event) => resetPage(() => setTo(event.target.value))} />
        <FilterSelect value={status} onChange={(value) => resetPage(() => setStatus(value as TransactionStatus | ""))}>
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
        <FilterSelect value={bulkStatus} onChange={(value) => setBulkStatus(value as TransactionStatus | "")}>
          <option value="">Bulk status</option>
          <option value="POSTED">Posted</option>
          <option value="EXCLUDED">Excluded</option>
        </FilterSelect>
        <Button
          disabled={selectedIds.length === 0 || bulkUpdate.isPending || (!bulkCategoryId && !bulkStatus)}
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

function TrendLineChart({ data }: { data: MonthlySummary[] }) {
  if (data.length === 0) {
    return <ChartEmpty label="No trend yet" />;
  }
  const values = data.map((item) => item.expense);
  const max = Math.max(...values, 1);
  const points = data.map((item, index) => {
    const x = data.length === 1 ? 50 : (index / (data.length - 1)) * 100;
    const y = 90 - (item.expense / max) * 75;
    return `${x},${y}`;
  });
  return (
    <div className="grid gap-3">
      <svg className="h-48 w-full overflow-visible" role="img" aria-label="Monthly spending trend" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={points.join(" ")} stroke="var(--primary)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
        {points.map((point) => {
          const [x, y] = point.split(",");
          return <circle key={point} cx={x} cy={y} fill="var(--background)" r="1.8" stroke="var(--primary)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />;
        })}
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground sm:grid-cols-6">
        {data.map((item) => (
          <span key={item.periodStart}>{new Date(item.periodStart).toLocaleDateString("en-IN", { month: "short" })}</span>
        ))}
      </div>
    </div>
  );
}

function CategoryDistribution({ data }: { data: CategorySpend[] }) {
  if (data.length === 0) {
    return <ChartEmpty label="No category spending this month" />;
  }
  return (
    <div className="grid gap-3">
      {data.slice(0, 6).map((item) => (
        <div key={item.categoryId ?? item.name} className="grid gap-1">
          <div className="flex items-center justify-between gap-3 text-sm">
            <span className="truncate font-medium">{item.name}</span>
            <span className="tabular-nums text-muted-foreground">{formatMoney(item.total)}</span>
          </div>
          <div className="h-2 overflow-hidden rounded-full bg-muted">
            <div
              className="h-full rounded-full bg-primary transition-[width] duration-500"
              style={{ width: `${Math.max(3, item.share)}%` }}
            />
          </div>
        </div>
      ))}
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
        <div key={item.periodStart} className="grid grid-cols-[3.5rem_1fr] items-center gap-3 text-sm">
          <span className="text-xs text-muted-foreground">
            {new Date(item.periodStart).toLocaleDateString("en-IN", { month: "short" })}
          </span>
          <div className="grid gap-1">
            <div className="h-2 rounded-full bg-success/20">
              <div className="h-full rounded-full bg-success" style={{ width: `${(item.income / max) * 100}%` }} />
            </div>
            <div className="h-2 rounded-full bg-primary/15">
              <div className="h-full rounded-full bg-primary" style={{ width: `${(item.expense / max) * 100}%` }} />
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
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">No transactions yet.</h2>
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
            <Button variant="outline" disabled={seedDemo.isPending} onClick={() => seedDemo.mutate()}>
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
            <WidgetLine label="Accounts" value={accountCount > 0 ? `${accountCount} connected` : "Waiting"} />
            <WidgetLine label="Transactions" value="Empty" />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <FinancialCard title="Account balance" value={formatMoney(0)} detail="No account balances yet." icon={Landmark} />
        <FinancialCard title="Spending overview" value="No data" detail="Charts appear after imports." icon={ReceiptText} />
        <FinancialCard title="Net cashflow" value={formatMoney(0)} detail="No cashflow yet." icon={CircleDollarSign} />
      </section>

      <TransactionList transactions={[]} emptyLabel="Your first transaction will appear here." />
    </main>
  );
}

function DashboardLoadingState() {
  return (
    <main className="grid gap-5">
      <Skeleton className="h-52 w-full" />
      <div className="grid gap-4 md:grid-cols-4">
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-36 w-full" />
      </div>
      <Skeleton className="h-96 w-full" />
    </main>
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
