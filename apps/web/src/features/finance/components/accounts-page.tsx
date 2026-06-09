"use client";

import { CreditCard, Landmark, LinkIcon, Loader2, Plus, RefreshCw, Wallet } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAccounts, useSeedDemoFinanceData } from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type { Account, AccountType } from "@/features/finance/types";

const accountIconMap = {
  CREDIT_CARD: CreditCard,
  WALLET: Wallet,
  SAVINGS: Landmark,
  CURRENT: Landmark,
  CASH: Landmark,
  OTHER: Landmark,
} satisfies Record<AccountType, typeof Landmark>;

export function AccountsPage() {
  const accountsQuery = useAccounts();
  const seedDemo = useSeedDemoFinanceData();
  const accounts = accountsQuery.data ?? [];

  if (accountsQuery.isLoading) {
    return (
      <main className="grid gap-5">
        <Skeleton className="h-48 w-full" />
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-44 w-full" />
          <Skeleton className="h-44 w-full" />
        </div>
      </main>
    );
  }

  return (
    <main className="grid gap-5">
      <section className="flex flex-col gap-4 rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="max-w-2xl space-y-2">
          <p className="text-sm font-medium text-muted-foreground">Connected accounts</p>
          <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
            {accounts.length > 0 ? "Accounts are ready for transaction ingestion." : "No accounts connected yet."}
          </h2>
          <p className="text-sm leading-6 text-muted-foreground">
            This page prepares the account layer for future SMS, CSV, and bank API ingestion without
            launching a bank connection flow in this phase.
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Button variant="outline" disabled={seedDemo.isPending} onClick={() => seedDemo.mutate()}>
            {seedDemo.isPending ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
            Seed local demo
          </Button>
          <Button render={<Link href="/dashboard" />}>
            <LinkIcon className="size-4" aria-hidden />
            Connect account
          </Button>
        </div>
      </section>

      {accounts.length === 0 ? (
        <section className="grid min-h-80 place-items-center rounded-lg border border-dashed border-border bg-muted/25 p-6 text-center">
          <div className="grid max-w-md gap-3">
            <span className="mx-auto grid size-12 place-items-center rounded-lg bg-card text-primary shadow-raised">
              <Plus className="size-5" aria-hidden />
            </span>
            <h3 className="text-lg font-semibold">Connect account CTA state</h3>
            <p className="text-sm leading-6 text-muted-foreground">
              The storage, account DTOs, and UI state are ready. The actual bank/SMS/CSV ingestion
              integrations remain intentionally out of scope.
            </p>
          </div>
        </section>
      ) : (
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {accounts.map((account) => (
            <AccountCard key={account.id} account={account} />
          ))}
        </section>
      )}
    </main>
  );
}

function AccountCard({ account }: { account: Account }) {
  const Icon = accountIconMap[account.accountType];

  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-4">
        <div className="min-w-0">
          <CardTitle className="truncate text-base">{account.displayName}</CardTitle>
          <p className="mt-1 truncate text-sm text-muted-foreground">
            {account.institutionName}
            {account.accountMask ? ` • ${account.accountMask}` : ""}
          </p>
        </div>
        <span className="grid size-10 place-items-center rounded-lg bg-primary/10 text-primary">
          <Icon className="size-5" aria-hidden />
        </span>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div>
          <p className="text-xs font-medium text-muted-foreground">Current balance</p>
          <p className="mt-1 text-2xl font-semibold tabular-nums">{formatMoney(account.currentBalance)}</p>
        </div>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <AccountMeta label="Status" value={account.status.toLowerCase()} />
          <AccountMeta label="Source" value={account.source.replace("_", " ").toLowerCase()} />
          <AccountMeta label="Type" value={account.accountType.replace("_", " ").toLowerCase()} />
          <AccountMeta
            label="Sync"
            value={account.lastSyncedAt ? "ready" : "waiting"}
            icon={RefreshCw}
          />
        </div>
      </CardContent>
    </Card>
  );
}

function AccountMeta({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: string;
  icon?: typeof RefreshCw;
}) {
  return (
    <div className="rounded-lg border border-border/70 bg-muted/25 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 flex items-center gap-1 truncate font-medium capitalize">
        {Icon ? <Icon className="size-3.5" aria-hidden /> : null}
        {value}
      </p>
    </div>
  );
}
