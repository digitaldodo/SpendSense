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

export type FinancialHealthIndicator = {
  key: string;
  label: string;
  state: BudgetState;
  value: number;
  benchmark: number;
  monthlyChange: number;
  explanation: string;
  actionHint: string;
};

export type FinancialHealthTrendPoint = {
  periodStart: string;
  income: number;
  expense: number;
  netCashflow: number;
  savingsRate: number;
  score: number;
  state: BudgetState;
};

export type FinancialHealthBreakdown = {
  generatedAt: string;
  state: BudgetState;
  score: number;
  headline: string;
  indicators: FinancialHealthIndicator[];
  trendHistory: FinancialHealthTrendPoint[];
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

export type ReportSummary = {
  income: number;
  expense: number;
  netCashflow: number;
  savingsRate: number;
  recurringSpend: number;
  anomalySpend: number;
};

export type DeterministicInsight = {
  type: string;
  state: BudgetState;
  title: string;
  body: string;
  primaryValue: number;
  comparisonValue: number;
  actionLabel: string;
};

export type RecurringPattern = {
  categoryId?: string | null;
  categoryName: string;
  merchantName: string;
  merchantNormalized: string;
  amount: number;
  currency: string;
  cadence: string;
  occurrenceCount: number;
  firstSeenOn: string;
  lastSeenOn: string;
  nextExpectedOn?: string | null;
  confidence: number;
  subscription: boolean;
};

export type SpendingAnomaly = {
  categoryId?: string | null;
  categoryName: string;
  state: BudgetState;
  currentSpend: number;
  baselineSpend: number;
  changePercent: number;
  absoluteChange: number;
  message: string;
};

export type MonthlyComparison = {
  periodStart: string;
  income: number;
  expense: number;
  netCashflow: number;
  incomeChange: number;
  expenseChange: number;
  expenseChangePercent: number;
  savingsRate: number;
};

export type CategoryTrendInsight = {
  categoryId?: string | null;
  categoryName: string;
  colorToken: string;
  currentSpend: number;
  previousAverage: number;
  changePercent: number;
  direction: "UP" | "DOWN" | "FLAT";
  state: BudgetState;
};

export type SavingsTrajectory = {
  periodStart: string;
  netSavings: number;
  savingsRate: number;
  cumulativeSavings: number;
};

export type IncomeStability = {
  state: BudgetState | "WAITING";
  averageIncome: number;
  averageDeviation: number;
  stabilityScore: number;
  monthsReviewed: number;
};

export type CategoryDeepDive = {
  categoryId?: string | null;
  categoryName: string;
  colorToken: string;
  totalSpend: number;
  averageMonthlySpend: number;
  latestMonthSpend: number;
  trendPercent: number;
  monthlyValues: MonthlyComparison[];
};

export type FinancialInsights = {
  generatedAt: string;
  periodStart: string;
  periodEnd: string;
  periodLabel: string;
  summary: ReportSummary;
  insights: DeterministicInsight[];
  anomalies: SpendingAnomaly[];
  recurringTransactions: RecurringPattern[];
  subscriptions: RecurringPattern[];
  monthlyComparisons: MonthlyComparison[];
  categoryTrends: CategoryTrendInsight[];
  savingsTrajectory: SavingsTrajectory[];
  incomeStability: IncomeStability;
  categoryDeepDives: CategoryDeepDive[];
};

export type DashboardInsightSummary = {
  recurringCount: number;
  subscriptionCount: number;
  subscriptionSpend: number;
  spendingSpikeCount: number;
  monthOverMonthExpenseChangePercent: number;
  largestExpenseChangeCategory: string;
  incomeConsistencyState: string;
  savingsTrendState: string;
};

export type GeneratedReport = {
  reportId: string;
  reportType: string;
  format: string;
  generatedAt: string;
  insights: FinancialInsights;
  categoryBreakdown: CategorySpend[];
};

export type Notification = {
  id: string;
  type: string;
  severity: "INFO" | "CAUTION" | "ACTION" | string;
  title: string;
  body: string;
  actionLabel?: string | null;
  actionUrl?: string | null;
  sourceType?: string | null;
  sourceId?: string | null;
  deliveryChannel: string;
  lifecycleStatus: string;
  priority: number;
  read: boolean;
  scheduledFor?: string | null;
  deliveredAt?: string | null;
  readAt?: string | null;
  dismissedAt?: string | null;
  expiresAt?: string | null;
  createdAt: string;
};

export type NotificationPreferences = {
  id: string;
  inAppEnabled: boolean;
  budgetWarningsEnabled: boolean;
  recurringRemindersEnabled: boolean;
  reportReadyEnabled: boolean;
  savingsNudgesEnabled: boolean;
  spendingIncreaseEnabled: boolean;
  weeklyDigestEnabled: boolean;
  monthlyReportEnabled: boolean;
  emailEnabled: boolean;
  emailAddress?: string | null;
  digestFrequency: "OFF" | "WEEKLY" | "MONTHLY" | string;
  budgetAlertEmailEnabled: boolean;
  recurringReminderEmailEnabled: boolean;
  reportEmailEnabled: boolean;
  deliveryFailureAlertsEnabled: boolean;
  timezone: string;
  quietHoursStart?: string | null;
  quietHoursEnd?: string | null;
  updatedAt: string;
};

export type NotificationPreferencePayload = Partial<
  Omit<NotificationPreferences, "id" | "updatedAt">
>;

export type ScheduledReport = {
  id: string;
  reportType: string;
  format: string;
  cadence: string;
  timezone: string;
  deliveryChannel: string;
  nextRunAt: string;
  lastRunAt?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ScheduledReportPayload = {
  reportType: string;
  format: string;
  cadence: string;
  timezone?: string;
  deliveryChannel?: string;
  active?: boolean;
};

export type ReportDeliveryLog = {
  id: string;
  scheduledReportId?: string | null;
  generatedReportId?: string | null;
  deliveryChannel: string;
  status: string;
  attemptedAt: string;
  deliveredAt?: string | null;
  errorMessage?: string | null;
};

export type DeliveryHistory = {
  id: string;
  notificationId?: string | null;
  scheduledReportId?: string | null;
  generatedReportId?: string | null;
  deliveryKind: string;
  channel: string;
  provider: string;
  recipient?: string | null;
  subject?: string | null;
  status: string;
  attemptCount: number;
  nextRetryAt?: string | null;
  lastAttemptAt?: string | null;
  deliveredAt?: string | null;
  failedAt?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  createdAt: string;
};

export type DeliveryRetry = {
  id: string;
  deliveryId: string;
  attemptNumber: number;
  scheduledFor: string;
  attemptedAt?: string | null;
  status: string;
  errorCode?: string | null;
  errorMessage?: string | null;
};

export type EmailPreview = {
  templateType: string;
  subject: string;
  html: string;
  text: string;
};

export type WorkerJobLog = {
  id: string;
  jobName: string;
  jobType: string;
  status: string;
  startedAt: string;
  finishedAt?: string | null;
  durationMs?: number | null;
  recordsScanned: number;
  recordsSucceeded: number;
  recordsFailed: number;
  heartbeatAt: string;
  errorMessage?: string | null;
};

export type SystemStatus = {
  status: string;
  observedAt: string;
  lastWorkerHeartbeatAt?: string | null;
  deliveriesLast24h: number;
  failedDeliveriesLast24h: number;
  pendingRetries: number;
  deliverySuccessRate: number;
  recentJobs: WorkerJobLog[];
};

export type NotificationSummary = {
  unreadCount: number;
  activeCount: number;
  latest: Notification[];
  timeline: Notification[];
};

export type NotificationDashboard = {
  unreadCount: number;
  upcomingSubscriptions: Notification[];
  budgetWarnings: Notification[];
  reminders: Notification[];
  scheduledReports: ScheduledReport[];
  savingsNudges: Notification[];
};

export type BudgetRollover = {
  budgetId: string;
  budgetName: string;
  categoryName: string;
  sourcePeriodStart: string;
  sourcePeriodEnd: string;
  targetPeriodStart: string;
  targetPeriodEnd: string;
  originalAmount: number;
  spentAmount: number;
  rolloverAmount: number;
  state: string;
};

export type CashflowImpactPoint = {
  monthStart: string;
  baselineFreeCashflow: number;
  simulatedFreeCashflow: number;
  projectedSavingsBalance: number;
};

export type AffordabilityScenarioPayload = {
  purchaseAmount: number;
  downPayment?: number;
  annualInterestRate: number;
  tenureMonths: number;
  existingMonthlyEmis?: number;
  goalId?: string;
  currency?: string;
};

export type AffordabilityScenario = {
  scenarioId: string;
  generatedAt: string;
  state: BudgetState;
  explanation: string;
  purchaseAmount: number;
  downPayment: number;
  financedAmount: number;
  monthlyEmi: number;
  totalInterest: number;
  totalPayment: number;
  safeEmiLimit: number;
  freeCashflowBefore: number;
  freeCashflowAfter: number;
  cashflowReductionPercent: number;
  savingsImpactOverTenure: number;
  goalDelayMonths?: number | null;
  delayedGoalName?: string | null;
  cashflowProjection: CashflowImpactPoint[];
};

export type ProjectionPayload = {
  months?: number;
  monthlySavingsOverride?: number;
  emergencyMonthlyExpenseOverride?: number;
};

export type ProjectionPoint = {
  monthStart: string;
  projectedBalance: number;
  cumulativeSavings: number;
  emergencyRunwayMonths: number;
};

export type FinancialProjection = {
  projectionId: string;
  generatedAt: string;
  state: BudgetState;
  currentBalance: number;
  monthlySavings: number;
  averageMonthlyExpense: number;
  emergencyRunwayMonths: number;
  fireStyleTarget: number;
  monthsToFireStyleTarget?: number | null;
  trajectory: ProjectionPoint[];
  notes: string[];
};

export type AiInsightCard = {
  type: string;
  state: BudgetState | "WAITING" | string;
  title: string;
  body: string;
  primaryValue: number;
  comparisonValue: number;
  actionLabel: string;
  actionIntent: string;
};

export type AiUsage = {
  provider: string;
  model: string;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  estimatedCostMinor: number;
  currency: string;
  latencyMs: number;
};

export type AiMessage = {
  id: string;
  conversationId: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  intent: string;
  content: string;
  insightCards: AiInsightCard[];
  followUpPrompts: string[];
  safetyFlags: string[];
  provider?: string | null;
  model?: string | null;
  promptTokens: number;
  completionTokens: number;
  latencyMs: number;
  createdAt: string;
};

export type AiConversationSummary = {
  id: string;
  title: string;
  status: string;
  contextScope: string;
  lastMessageAt: string;
  createdAt: string;
  lastMessagePreview?: string | null;
};

export type AiConversationDetail = {
  conversation: AiConversationSummary;
  messages: AiMessage[];
};

export type AiChatPayload = {
  conversationId?: string;
  prompt?: string;
  intent?: string;
  sourceTransactionId?: string;
  sourceBudgetId?: string;
  sourceGoalId?: string;
};

export type AiChatResponse = {
  conversation: AiConversationSummary;
  userMessage: AiMessage;
  assistantMessage: AiMessage;
  insightCards: AiInsightCard[];
  followUpPrompts: string[];
  usage: AiUsage;
  grounded: boolean;
  safetyLevel: string;
  citations: string[];
};

export type AiFeedbackPayload = {
  rating?: number;
  feedbackType?: string;
  comment?: string;
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
  insightSummary: DashboardInsightSummary;
  notificationDashboard: NotificationDashboard;
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
