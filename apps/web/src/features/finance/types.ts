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

export type BudgetState = "HEALTHY" | "CAUTION" | "RISK";

export type Budget = {
  id: string;
  name: string;
  category: Category;
  amount: number;
  currency: string;
  periodStart: string;
  periodEnd: string;
  rolloverEnabled: boolean;
  active: boolean;
  spent: number;
  remaining: number;
  usagePercent: number;
  state: BudgetState;
};

export type BudgetHistory = {
  id: string;
  budgetId?: string | null;
  budgetName?: string | null;
  categoryName?: string | null;
  action: string;
  previousAmount?: number | null;
  newAmount?: number | null;
  previousName?: string | null;
  newName?: string | null;
  periodStart?: string | null;
  periodEnd?: string | null;
  reason?: string | null;
  createdAt: string;
};

export type SavingsGoalStatus = "ACTIVE" | "COMPLETED" | "PAUSED" | "ARCHIVED";

export type GoalContribution = {
  id: string;
  amount: number;
  contributedOn: string;
  note?: string | null;
  createdAt: string;
};

export type SavingsGoal = {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  currency: string;
  targetDate?: string | null;
  status: SavingsGoalStatus;
  colorToken: string;
  iconName: string;
  progressPercent: number;
  remainingAmount: number;
  monthlyTarget: number;
  timelineState: string;
  completedAt?: string | null;
  recentContributions: GoalContribution[];
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
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export type CategorySpend = {
  categoryId?: string | null;
  name: string;
  colorToken: string;
  total: number;
  transactionCount: number;
  share: number;
};

export type MonthlySummary = {
  periodStart: string;
  income: number;
  expense: number;
  netCashflow: number;
};

export type BudgetOverview = {
  totalBudgeted: number;
  totalSpent: number;
  totalRemaining: number;
  usagePercent: number;
  overspentCount: number;
  state: BudgetState;
  budgets: Budget[];
};

export type FinancialHealth = {
  score: number;
  state: BudgetState;
  savingsRatio: number;
  spendingConsistency: number;
  incomeExpenseStability: number;
  overspendingFrequency: number;
};

export type SavingsMomentum = {
  monthNetSavings: number;
  goalContributionsThisMonth: number;
  savingsRatio: number;
  state: BudgetState;
};

export type CategoryTrend = {
  categoryId?: string | null;
  name: string;
  colorToken: string;
  periodStart: string;
  total: number;
};

export type DashboardFinanceSummary = {
  accountCount: number;
  transactionCount: number;
  demoSeeded: boolean;
  totalBalance: number;
  monthSpend: number;
  monthIncome: number;
  netCashflow: number;
  accounts: Account[];
  recentTransactions: Transaction[];
  categoryBreakdown: CategorySpend[];
  monthlySummary: MonthlySummary[];
  recentImports: import("@/features/ingestion/types").ImportJob[];
  budgetOverview: BudgetOverview;
  topOverspendingCategories: CategorySpend[];
  savingsGoals: SavingsGoal[];
  financialHealth: FinancialHealth;
  savingsMomentum: SavingsMomentum;
  categoryTrends: CategoryTrend[];
};

export type DemoSeedResult = {
  accountsCreated: number;
  transactionsCreated: number;
  alreadySeeded: boolean;
};

export type TransactionUpdatePayload = {
  categoryId?: string | null;
  status?: TransactionStatus;
  reason?: string;
};

export type BulkTransactionActionPayload = TransactionUpdatePayload & {
  transactionIds: string[];
};

export type BulkTransactionActionResult = {
  requested: number;
  updated: number;
};

export type AccountMergePayload = {
  targetAccountId: string;
  reason?: string;
};

export type BalanceCorrectionPayload = {
  correctedBalance: number;
  reason?: string;
};

export type BudgetPayload = {
  categoryId: string;
  name: string;
  amount: number;
  currency?: string;
  startsOn?: string;
  rolloverEnabled?: boolean;
  reason?: string;
};

export type SavingsGoalPayload = {
  name: string;
  targetAmount: number;
  currentAmount?: number;
  currency?: string;
  targetDate?: string;
  status?: SavingsGoalStatus;
  colorToken?: string;
  iconName?: string;
};

export type GoalContributionPayload = {
  amount: number;
  contributedOn?: string;
  note?: string;
};

export type CategoryPayload = {
  name: string;
  colorToken?: string;
  iconName?: string;
  reason?: string;
};

export type CategoryMergePayload = {
  targetCategoryId: string;
  reason?: string;
};
