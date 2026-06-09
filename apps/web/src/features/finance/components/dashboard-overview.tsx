"use client";

import { useMemo, useState } from "react";
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
  useCategories,
  useDashboardFinanceSummary,
  useSeedDemoFinanceData,
  useTransactions,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type { TransactionDirection, TransactionStatus } from "@/features/finance/types";

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
      <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                {summary.demoSeeded ? "Demo sample seeded user" : "Real transaction user"}
              </p>
              <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
                Your transaction workspace is live.
              </h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                Accounts, transactions, categories, and ingestion metadata are now flowing through the
                foundation layer.
              </p>
            </div>
            <Button variant="outline" render={<Link href="/accounts" />}>
              <LinkIcon className="size-4" aria-hidden />
              Accounts
            </Button>
          </div>
        </div>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-sm font-medium text-muted-foreground">Dashboard widgets shell</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            <WidgetLine label="Spending overview" value="Monthly totals only" />
            <WidgetLine label="Recent transactions" value={`${summary.recentTransactions.length} visible`} />
            <WidgetLine label="Account balance" value={`${summary.accountCount} account(s)`} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <FinancialCard
          title="Account balance"
          value={formatMoney(summary.totalBalance)}
          detail="Simple sum of connected account balances."
          icon={Landmark}
        />
        <FinancialCard
          title="This month spent"
          value={formatMoney(summary.monthSpend)}
          detail="Posted debit transactions for the current month."
          icon={ReceiptText}
        />
        <FinancialCard
          title="This month received"
          value={formatMoney(summary.monthIncome)}
          detail="Posted credit transactions for the current month."
          icon={CircleDollarSign}
        />
      </section>

      <TransactionExplorer />
    </main>
  );
}

function TransactionExplorer() {
  const accountsQuery = useAccounts();
  const categoriesQuery = useCategories();
  const [search, setSearch] = useState("");
  const [accountId, setAccountId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [direction, setDirection] = useState<TransactionDirection | "">("");
  const [status, setStatus] = useState<TransactionStatus | "">("");
  const [sort, setSort] = useState("-occurredAt");
  const [page, setPage] = useState(0);

  const filters = useMemo(
    () => ({
      search,
      accountId,
      categoryId,
      direction: direction || undefined,
      status: status || undefined,
      page,
      size: pageSize,
      sort,
    }),
    [accountId, categoryId, direction, page, search, sort, status]
  );
  const transactionsQuery = useTransactions(filters);
  const transactionPage = transactionsQuery.data;

  function resetPage(update: () => void) {
    update();
    setPage(0);
  }

  return (
    <section className="grid gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold">Transactions</h2>
          <p className="text-sm text-muted-foreground">Search and filters are wired to the paginated API.</p>
        </div>
        <Button
          variant="ghost"
          onClick={() => {
            setSearch("");
            setAccountId("");
            setCategoryId("");
            setDirection("");
            setStatus("");
            setSort("-occurredAt");
            setPage(0);
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
            placeholder="Search merchant or account"
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
        <FilterSelect
          className="md:col-start-5"
          value={status}
          onChange={(value) => resetPage(() => setStatus(value as TransactionStatus | ""))}
        >
          <option value="">All statuses</option>
          <option value="POSTED">Posted</option>
          <option value="PENDING">Pending</option>
        </FilterSelect>
      </div>

      {transactionsQuery.isLoading ? (
        <div className="grid gap-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : (
        <TransactionList transactions={transactionPage?.items ?? []} />
      )}

      <div className="flex items-center justify-between gap-3">
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
              Connect account slots, transaction storage, categories, and ingestion sessions are ready.
              The workspace stays quiet until real or local demo data exists.
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button render={<Link href="/accounts" />}>
              <LinkIcon className="size-4" aria-hidden />
              Connect account
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
            <CardTitle className="text-sm font-medium">Onboarding completion</CardTitle>
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
        <FinancialCard title="Spending overview" value="No data" detail="No chart is shown before transactions exist." icon={ReceiptText} />
        <FinancialCard title="Recent transactions" value="Empty" detail="The first ingested transaction will appear here." icon={CircleDollarSign} />
      </section>

      <TransactionList transactions={[]} emptyLabel="Your first transaction will appear here." />
    </main>
  );
}

function DashboardLoadingState() {
  return (
    <main className="grid gap-5">
      <Skeleton className="h-52 w-full" />
      <div className="grid gap-4 md:grid-cols-3">
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
  children: React.ReactNode;
  className?: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <select
      className={`h-10 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50 ${className ?? ""}`}
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
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
