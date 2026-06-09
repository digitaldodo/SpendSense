"use client";

import { CreditCard, Landmark, LinkIcon, Loader2, Merge, Plus, RefreshCw, Save, Wallet } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useCorrectAccountBalance,
  useMergeAccount,
  useAccounts,
  useSeedDemoFinanceData,
} from "@/features/finance/hooks/use-finance";
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
            <AccountCard key={account.id} account={account} accounts={accounts} />
          ))}
        </section>
      )}
    </main>
  );
}

function AccountCard({ account, accounts }: { account: Account; accounts: Account[] }) {
  const Icon = accountIconMap[account.accountType];
  const mergeAccount = useMergeAccount(account.id);
  const correctBalance = useCorrectAccountBalance(account.id);
  const [targetAccountId, setTargetAccountId] = useState("");
  const [correctedBalance, setCorrectedBalance] = useState(String(account.currentBalance));
  const mergeTargets = accounts.filter((candidate) => candidate.id !== account.id && candidate.status === "ACTIVE");

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
        <div className="grid gap-2 rounded-lg border border-border/70 bg-muted/20 p-3">
          <p className="text-sm font-medium">Balance correction</p>
          <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
            <Input
              className="h-9"
              inputMode="decimal"
              value={correctedBalance}
              onChange={(event) => setCorrectedBalance(event.target.value)}
            />
            <Button
              size="sm"
              disabled={correctBalance.isPending || Number.isNaN(Number(correctedBalance))}
              onClick={() =>
                correctBalance.mutate({
                  correctedBalance: Number(correctedBalance),
                  reason: "Manual balance correction",
                })
              }
            >
              {correctBalance.isPending ? (
                <Loader2 className="size-4 animate-spin" aria-hidden />
              ) : (
                <Save className="size-4" aria-hidden />
              )}
              Save
            </Button>
          </div>
        </div>
        {mergeTargets.length > 0 ? (
          <div className="grid gap-2 rounded-lg border border-border/70 bg-muted/20 p-3">
            <p className="text-sm font-medium">Merge account</p>
            <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
              <select
                className="h-9 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-3 focus:ring-ring/50"
                value={targetAccountId}
                onChange={(event) => setTargetAccountId(event.target.value)}
              >
                <option value="">Select target</option>
                {mergeTargets.map((target) => (
                  <option key={target.id} value={target.id}>
                    {target.displayName}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                variant="outline"
                disabled={mergeAccount.isPending || !targetAccountId}
                onClick={() =>
                  mergeAccount.mutate(
                    {
                      targetAccountId,
                      reason: "Manual account merge",
                    },
                    { onSuccess: () => setTargetAccountId("") }
                  )
                }
              >
                {mergeAccount.isPending ? (
                  <Loader2 className="size-4 animate-spin" aria-hidden />
                ) : (
                  <Merge className="size-4" aria-hidden />
                )}
                Merge
              </Button>
            </div>
          </div>
        ) : null}
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
