export type AccountType = "SAVINGS" | "CURRENT" | "CREDIT_CARD" | "WALLET" | "CASH" | "OTHER";
export type AccountStatus = "ACTIVE" | "PAUSED" | "DISCONNECTED";
export type IngestionSource = "MANUAL" | "SMS" | "CSV" | "BANK_API" | "DEMO";
export type TransactionDirection = "DEBIT" | "CREDIT";
export type TransactionStatus = "PENDING" | "POSTED" | "EXCLUDED";

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type Account = {
  id: string;
  displayName: string;
  institutionName: string;
  accountType: AccountType;
  accountMask?: string | null;
  currency: string;
  currentBalance: number;
  availableBalance?: number | null;
  status: AccountStatus;
  source: IngestionSource;
  connectedAt: string;
  lastSyncedAt?: string | null;
};

export type Category = {
  id: string;
  name: string;
  slug: string;
  colorToken: string;
  iconName: string;
  systemCategory: boolean;
};

export type Transaction = {
  id: string;
  amount: number;
  currency: string;
  direction: TransactionDirection;
  status: TransactionStatus;
  occurredAt: string;
  bookedAt?: string | null;
  merchantName: string;
  merchantNormalized: string;
  description?: string | null;
  reference?: string | null;
  source: IngestionSource;
  account: Account;
  category?: Category | null;
};

export type TransactionDetail = Transaction & {
  sourceTransactionId?: string | null;
  idempotencyKey?: string | null;
  dedupeFingerprint: string;
  ingestionSessionId?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type TransactionFilters = {
  search?: string;
  accountId?: string;
  categoryId?: string;
  direction?: TransactionDirection;
  status?: TransactionStatus;
  page?: number;
  size?: number;
  sort?: string;
};

export type DashboardFinanceSummary = {
  accountCount: number;
  transactionCount: number;
  demoSeeded: boolean;
  totalBalance: number;
  monthSpend: number;
  monthIncome: number;
  accounts: Account[];
  recentTransactions: Transaction[];
};

export type DemoSeedResult = {
  accountsCreated: number;
  transactionsCreated: number;
  alreadySeeded: boolean;
};
