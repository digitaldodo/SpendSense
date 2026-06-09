"use client";

import { useState } from "react";
import { ArrowDownLeft, ArrowUpRight, ReceiptText } from "lucide-react";
import { CategoryBadge } from "@/features/finance/components/category-badge";
import { TransactionDetailSheet } from "@/features/finance/components/transaction-detail-sheet";
import {
  formatAccountLabel,
  formatMerchantName,
  formatTransactionAmount,
  formatTransactionTime,
  groupTransactionsByDate,
} from "@/features/finance/lib/format";
import type { Transaction } from "@/features/finance/types";
import { cn } from "@/lib/utils";

export function TransactionList({
  transactions,
  emptyLabel = "No transactions match this view.",
  selectedIds = [],
  onToggleSelected,
}: {
  transactions: Transaction[];
  emptyLabel?: string;
  selectedIds?: string[];
  onToggleSelected?: (transactionId: string) => void;
}) {
  const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);
  const groups = groupTransactionsByDate(transactions);

  if (transactions.length === 0) {
    return (
      <div className="grid min-h-56 place-items-center rounded-lg border border-dashed border-border bg-muted/25 p-6 text-center">
        <div className="grid max-w-sm gap-2">
          <span className="mx-auto grid size-11 place-items-center rounded-lg bg-card text-primary shadow-raised">
            <ReceiptText className="size-5" aria-hidden />
          </span>
          <p className="text-sm font-medium">{emptyLabel}</p>
          <p className="text-sm leading-6 text-muted-foreground">
            Transactions will appear here once an ingestion source or local demo seed adds them.
          </p>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="grid gap-5">
        {groups.map((group) => (
          <section key={group.label} className="grid gap-2">
            <h3 className="px-1 text-xs font-semibold uppercase tracking-normal text-muted-foreground">
              {group.label}
            </h3>
            <div className="overflow-hidden rounded-lg border border-border bg-card shadow-raised">
              {group.items.map((transaction, index) => {
                const isCredit = transaction.direction === "CREDIT";
                const Icon = isCredit ? ArrowDownLeft : ArrowUpRight;
                const selected = selectedIds.includes(transaction.id);
                return (
                  <div
                    key={transaction.id}
                    className={cn(
                      "grid w-full grid-cols-[auto_2.5rem_1fr_auto] items-center gap-3 px-3 py-3 text-left transition-colors hover:bg-muted/55 sm:grid-cols-[auto_2.5rem_1fr_auto_auto]",
                      index > 0 && "border-t border-border/70"
                    )}
                  >
                    {onToggleSelected ? (
                      <input
                        aria-label={`Select ${transaction.merchantName}`}
                        className="size-4 rounded border-border"
                        type="checkbox"
                        checked={selected}
                        onChange={() => onToggleSelected(transaction.id)}
                      />
                    ) : (
                      <span className="hidden" />
                    )}
                    <span
                      className={cn(
                        "grid size-10 place-items-center rounded-lg",
                        isCredit ? "bg-success/10 text-success" : "bg-muted text-primary"
                      )}
                    >
                      <Icon className="size-4" aria-hidden />
                    </span>
                    <button className="min-w-0 text-left" onClick={() => setSelectedTransactionId(transaction.id)}>
                      <span className="block truncate text-sm font-semibold">
                        {formatMerchantName(transaction.merchantName)}
                      </span>
                      <span className="mt-1 block truncate text-xs text-muted-foreground">
                        {formatAccountLabel(transaction.account)} · {formatTransactionTime(transaction.occurredAt)}
                      </span>
                    </button>
                    <span className="hidden sm:block">
                      <CategoryBadge category={transaction.category} />
                    </span>
                    <span
                      className={cn(
                        "text-right text-sm font-semibold tabular-nums",
                        isCredit ? "text-success" : "text-foreground"
                      )}
                    >
                      {formatTransactionAmount(transaction)}
                    </span>
                  </div>
                );
              })}
            </div>
          </section>
        ))}
      </div>

      <TransactionDetailSheet
        transactionId={selectedTransactionId}
        open={Boolean(selectedTransactionId)}
        onOpenChange={(open) => {
          if (!open) setSelectedTransactionId(null);
        }}
      />
    </>
  );
}
