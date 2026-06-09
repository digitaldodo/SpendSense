import type { Transaction } from "@/features/finance/types";

const INR_FORMATTER = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

const DATE_FORMATTER = new Intl.DateTimeFormat("en-IN", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

const TIME_FORMATTER = new Intl.DateTimeFormat("en-IN", {
  hour: "2-digit",
  minute: "2-digit",
});

export function formatMoney(amount: number, currency = "INR") {
  if (currency === "INR") {
    return INR_FORMATTER.format(amount);
  }

  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(amount);
}

export function formatTransactionAmount(transaction: Pick<Transaction, "amount" | "currency" | "direction">) {
  const prefix = transaction.direction === "CREDIT" ? "+" : "-";
  return `${prefix}${formatMoney(transaction.amount, transaction.currency)}`;
}

export function formatMerchantName(value: string) {
  const cleaned = value.replace(/\s+/g, " ").trim();
  if (!cleaned) {
    return "Unknown merchant";
  }

  if (cleaned === cleaned.toUpperCase() || cleaned === cleaned.toLowerCase()) {
    return cleaned
      .split(" ")
      .map((part) => (part.length <= 3 ? part.toUpperCase() : part[0].toUpperCase() + part.slice(1).toLowerCase()))
      .join(" ");
  }

  return cleaned;
}

export function formatAccountLabel(account: { displayName: string; accountMask?: string | null }) {
  return account.accountMask ? `${account.displayName} • ${account.accountMask}` : account.displayName;
}

export function formatTransactionDate(value: string) {
  return DATE_FORMATTER.format(new Date(value));
}

export function formatTransactionTime(value: string) {
  return TIME_FORMATTER.format(new Date(value));
}

export function getDateGroupLabel(value: string) {
  const date = new Date(value);
  const today = new Date();
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const startOfDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const dayDelta = Math.round((startOfToday.getTime() - startOfDate.getTime()) / 86_400_000);

  if (dayDelta === 0) return "Today";
  if (dayDelta === 1) return "Yesterday";
  if (dayDelta < 7) return "This week";
  return DATE_FORMATTER.format(date);
}

export function groupTransactionsByDate(transactions: Transaction[]) {
  return transactions.reduce<Array<{ label: string; items: Transaction[] }>>((groups, transaction) => {
    const label = getDateGroupLabel(transaction.occurredAt);
    const existing = groups.find((group) => group.label === label);
    if (existing) {
      existing.items.push(transaction);
      return groups;
    }
    return [...groups, { label, items: [transaction] }];
  }, []);
}
