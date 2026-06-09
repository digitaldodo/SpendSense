"use client";

import { CalendarDays, Fingerprint, Landmark, Loader2, ReceiptText, Save, ShieldCheck } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { CategoryBadge } from "@/features/finance/components/category-badge";
import {
  useCategories,
  useTransactionDetail,
  useUpdateTransaction,
} from "@/features/finance/hooks/use-finance";
import {
  formatAccountLabel,
  formatMerchantName,
  formatMoney,
  formatTransactionAmount,
  formatTransactionDate,
  formatTransactionTime,
} from "@/features/finance/lib/format";

export function TransactionDetailSheet({
  transactionId,
  open,
  onOpenChange,
}: {
  transactionId?: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const detailQuery = useTransactionDetail(open ? transactionId : null);
  const categoriesQuery = useCategories();
  const updateTransaction = useUpdateTransaction(transactionId);
  const transaction = detailQuery.data;
  const [draftCategoryId, setDraftCategoryId] = useState<string | null>(null);
  const categoryId = draftCategoryId ?? transaction?.category?.id ?? "";

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full max-w-md gap-0 overflow-y-auto sm:max-w-md">
        <SheetHeader className="border-b border-border/70 p-5 pr-12">
          <SheetDescription>Transaction detail</SheetDescription>
          <SheetTitle className="text-xl">
            {transaction ? formatMerchantName(transaction.merchantName) : "Loading transaction"}
          </SheetTitle>
        </SheetHeader>

        {detailQuery.isLoading ? (
          <div className="grid gap-4 p-5">
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : transaction ? (
          <div className="grid gap-5 p-5">
            <div className="rounded-lg border border-border bg-muted/35 p-4">
              <p
                className={
                  transaction.direction === "CREDIT"
                    ? "text-3xl font-semibold text-success"
                    : "text-3xl font-semibold text-foreground"
                }
              >
                {formatTransactionAmount(transaction)}
              </p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <CategoryBadge category={transaction.category} />
                <span className="rounded-md border border-border bg-background px-2 py-1 text-xs text-muted-foreground">
                  {transaction.status.toLowerCase()}
                </span>
              </div>
            </div>

            <div className="grid gap-3">
              <DetailLine
                icon={CalendarDays}
                label="Date"
                value={`${formatTransactionDate(transaction.occurredAt)} at ${formatTransactionTime(
                  transaction.occurredAt
                )}`}
              />
              <DetailLine
                icon={Landmark}
                label="Account"
                value={`${formatAccountLabel(transaction.account)} · ${transaction.account.institutionName}`}
              />
              <DetailLine
                icon={ReceiptText}
                label="Description"
                value={transaction.description || transaction.reference || "No description provided"}
              />
              <DetailLine icon={ShieldCheck} label="Source" value={transaction.source.replace("_", " ")} />
              <DetailLine icon={Fingerprint} label="Dedupe fingerprint" value={transaction.dedupeFingerprint} mono />
            </div>

            <div className="rounded-lg border border-border bg-card p-4">
              <div className="grid gap-2">
                <p className="text-sm font-medium">Category</p>
                <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                  <select
                    className="h-10 min-w-0 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-ring focus:ring-3 focus:ring-ring/50"
                    value={categoryId}
                    onChange={(event) => setDraftCategoryId(event.target.value)}
                  >
                    <option value="">Uncategorized</option>
                    {(categoriesQuery.data ?? []).map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                  <Button
                    disabled={updateTransaction.isPending || categoryId === (transaction.category?.id ?? "")}
                    onClick={() =>
                      updateTransaction.mutate(
                        {
                          categoryId: categoryId || null,
                          reason: "Manual category edit",
                        },
                        { onSuccess: () => setDraftCategoryId(null) }
                      )
                    }
                  >
                    {updateTransaction.isPending ? (
                      <Loader2 className="size-4 animate-spin" aria-hidden />
                    ) : (
                      <Save className="size-4" aria-hidden />
                    )}
                    Save
                  </Button>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-border bg-card p-4">
              <p className="text-sm font-medium">Ingestion-ready metadata</p>
              <dl className="mt-3 grid gap-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Idempotency key</dt>
                  <dd className="max-w-44 truncate font-mono text-xs">
                    {transaction.idempotencyKey ?? "Not provided"}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Source ID</dt>
                  <dd className="max-w-44 truncate font-mono text-xs">
                    {transaction.sourceTransactionId ?? "Not provided"}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Booked amount</dt>
                  <dd>{formatMoney(transaction.amount, transaction.currency)}</dd>
                </div>
              </dl>
            </div>
          </div>
        ) : (
          <div className="p-5 text-sm text-muted-foreground">Transaction could not be loaded.</div>
        )}
      </SheetContent>
    </Sheet>
  );
}

function DetailLine({
  icon: Icon,
  label,
  value,
  mono,
}: {
  icon: typeof CalendarDays;
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="grid grid-cols-[2rem_1fr] gap-3 rounded-lg border border-border/80 bg-card p-3">
      <span className="grid size-8 place-items-center rounded-lg bg-muted text-primary">
        <Icon className="size-4" aria-hidden />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        <p className={mono ? "truncate font-mono text-xs" : "truncate text-sm font-medium"}>{value}</p>
      </div>
    </div>
  );
}
