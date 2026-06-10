import { NextResponse, type NextRequest } from "next/server";

export async function GET(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return handleRequest(request, context);
}

export async function POST(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return handleRequest(request, context);
}

export async function PATCH(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  return handleRequest(request, context);
}

export async function OPTIONS() {
  return new NextResponse(null, { status: 204, headers: corsHeaders() });
}

async function handleRequest(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  if (!ciMockApiEnabled()) {
    return NextResponse.json({ message: "Not found" }, { status: 404 });
  }

  const { path } = await context.params;
  const route = `/${path.join("/")}`;
  const body = await fixtureFor(route, request);
  return NextResponse.json(
    { success: true, data: body, message: "ok", traceId: "ci-lighthouse-trace" },
    { headers: corsHeaders() }
  );
}

async function fixtureFor(route: string, request: NextRequest) {
  if (route === "/profile/current") {
    return profileFixture;
  }
  if (route === "/health/live" || route === "/health/heartbeat" || route === "/health/ready") {
    return {
      status: "UP",
      service: "spendsense-api",
      environment: "development",
      version: "ci",
      commit: "ci",
      maintenanceMode: false,
      degradedMode: false,
      checkedAt: new Date().toISOString(),
      checks: { application: "UP", database: "UP", cors: "UP" },
    };
  }
  if (route === "/health/version") {
    return {
      service: "spendsense-api",
      environment: "development",
      version: "ci",
      commit: "ci",
      maintenanceMode: false,
      degradedMode: false,
      featureFlags: "{}",
      alertEscalationEmail: "",
      reportedAt: new Date().toISOString(),
    };
  }
  if (route === "/transactions/dashboard-summary") {
    return dashboardFixture;
  }
  if (route === "/actions/dashboard") {
    return smartActionDashboardFixture;
  }
  if (route.startsWith("/actions/")) {
    return smartActionDashboardFixture.actions[0];
  }
  if (route === "/accounts") {
    return dashboardFixture.accounts;
  }
  if (route === "/categories") {
    return categoriesFixture;
  }
  if (route === "/transactions") {
    return { items: [], totalItems: 0, page: 0, size: 12, hasNext: false };
  }
  if (route.startsWith("/planning/")) {
    return [];
  }
  if (route === "/notifications/preferences") {
    return notificationPreferencesFixture;
  }
  if (route === "/notifications/system-status") {
    return { status: "HEALTHY", deliverySuccessRate: 99, pendingRetries: 0 };
  }
  if (route === "/onboarding/progress") {
    return request.method === "POST" ? profileFixture : profileFixture.onboardingProgress;
  }
  if (route === "/onboarding/complete") {
    return profileFixture;
  }
  if (route === "/notifications/summary") {
    return { unreadCount: 0, activeCount: 0, latest: [], timeline: [] };
  }
  if (route === "/notifications") {
    return [];
  }
  return null;
}

function ciMockApiEnabled() {
  return (
    process.env.SPENDSENSE_ENABLE_CI_MOCK_API === "1" &&
    process.env.NEXT_PUBLIC_APP_ENV !== "production"
  );
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Credentials": "true",
    "Access-Control-Allow-Headers": "authorization,content-type,x-correlation-id",
    "Access-Control-Allow-Methods": "GET,POST,PATCH,DELETE,OPTIONS",
    "Access-Control-Allow-Origin": "*",
  };
}

const profileFixture = {
  id: "20000000-0000-4000-8000-000000000001",
  email: "ci@spendsense.local",
  displayName: "SpendSense CI",
  roles: ["USER", "ADMIN"],
  onboardingCompleted: true,
  onboardingProgress: {
    currentStep: 0,
    completedSteps: [],
  },
  financialPreferences: {
    salaryRange: "RANGE_100K_150K",
    employmentType: "SALARIED",
    monthlyFixedExpenses: 42000,
    goals: ["EMERGENCY_FUND"],
    spendingHabits: ["PLANNED"],
    riskComfort: "BALANCED",
  },
};

const categoriesFixture = [
  {
    id: "cat-food",
    name: "Food",
    colorToken: "green",
    iconName: "tag",
    systemCategory: true,
  },
  {
    id: "cat-travel",
    name: "Travel",
    colorToken: "blue",
    iconName: "wallet",
    systemCategory: true,
  },
];

const dashboardFixture = {
  demoSeeded: false,
  accountCount: 2,
  transactionCount: 24,
  totalBalance: 185000,
  monthSpend: 42000,
  monthIncome: 125000,
  netCashflow: 83000,
  recentImports: [
    {
      id: "import-1",
      originalFilename: "production-readiness.csv",
      recordsImported: 24,
      recordsDuplicate: 2,
    },
  ],
  accounts: [
    { id: "account-1", displayName: "Salary account", currentBalance: 160000, currency: "INR" },
    { id: "account-2", displayName: "Travel card", currentBalance: 25000, currency: "INR" },
  ],
  recentTransactions: [],
  monthlySummary: [
    { periodStart: "2026-01-01T00:00:00Z", income: 120000, expense: 38000 },
    { periodStart: "2026-02-01T00:00:00Z", income: 122000, expense: 41000 },
    { periodStart: "2026-03-01T00:00:00Z", income: 125000, expense: 42000 },
  ],
  categoryBreakdown: [
    {
      categoryId: "cat-food",
      name: "Food",
      colorToken: "green",
      total: 18000,
      share: 43,
      transactionCount: 8,
    },
    {
      categoryId: "cat-travel",
      name: "Travel",
      colorToken: "blue",
      total: 12000,
      share: 29,
      transactionCount: 4,
    },
  ],
  categoryTrends: [],
  insightSummary: {
    subscriptionCount: 3,
    subscriptionSpend: 2499,
    spendingSpikeCount: 1,
    monthOverMonthExpenseChangePercent: 3,
    incomeConsistencyState: "HEALTHY",
    savingsTrendState: "HEALTHY",
    largestExpenseChangeCategory: "Food",
  },
  notificationDashboard: {
    upcomingSubscriptions: [],
    budgetWarnings: [],
    scheduledReports: [],
    savingsNudges: [],
  },
  budgets: [],
  savingsGoals: [],
};

const smartActionDashboardFixture = {
  generatedAt: "2026-06-11T00:00:00Z",
  dailySummary: {
    headline: "You are protecting positive cashflow.",
    monthIncome: 125000,
    monthSpend: 42000,
    netCashflow: 83000,
    savingsRate: 66,
    tone: "SUPPORTIVE",
    explanation: "Uses current month posted income minus posted debits; excluded transactions are not counted.",
  },
  todayFocus: {
    title: "Move a calm surplus into savings",
    body: "Your month-to-date cashflow is positive. Moving INR 5000 keeps the action grounded in actual surplus.",
    focusType: "SMART_SAVINGS",
    impactAmount: 5000,
    actionId: "action-1",
  },
  actions: [
    {
      id: "action-1",
      actionType: "SMART_SAVINGS",
      category: "SAVINGS",
      status: "OPEN",
      priority: 82,
      title: "Move a calm surplus into savings",
      body: "Your month-to-date cashflow is positive. Moving INR 5000 keeps the action grounded in actual surplus.",
      explanation: "Calculated as 25% of current positive net cashflow, capped at INR 5000. No future income or investment return is assumed.",
      impactAmount: 5000,
      impactPercent: 4,
      currency: "INR",
      sourceType: "MONTHLY_CASHFLOW",
      sourceId: "2026-06",
      dueOn: "2026-06-13",
      snoozedUntil: null,
      completedAt: null,
      dismissedAt: null,
      generatedAt: "2026-06-11T00:00:00Z",
    },
    {
      id: "action-2",
      actionType: "SUBSCRIPTION_CLEANUP",
      category: "SUBSCRIPTIONS",
      status: "OPEN",
      priority: 70,
      title: "Review recurring subscriptions",
      body: "3 recurring payments total INR 2499 and represent 6% of current month spending.",
      explanation: "Recurring payments are detected from repeated posted debits with similar amount and cadence.",
      impactAmount: 2499,
      impactPercent: 6,
      currency: "INR",
      sourceType: "RECURRING_DEBITS",
      sourceId: "2026-06",
      dueOn: "2026-06-16",
      snoozedUntil: null,
      completedAt: null,
      dismissedAt: null,
      generatedAt: "2026-06-11T00:00:00Z",
    },
  ],
  streaks: [
    {
      id: "streak-1",
      streakKey: "daily_spend_run_rate",
      label: "Daily spending stayed within run-rate",
      currentCount: 4,
      bestCount: 4,
      unit: "days",
      state: "MOMENTUM",
      lastQualifiedOn: "2026-06-11",
      explanation: "4 day(s) in a row stayed at or below the deterministic daily run-rate of INR 1500.",
    },
    {
      id: "streak-2",
      streakKey: "positive_cashflow_months",
      label: "Positive cashflow months",
      currentCount: 3,
      bestCount: 3,
      unit: "months",
      state: "MOMENTUM",
      lastQualifiedOn: "2026-06-01",
      explanation: "Income has stayed ahead of posted spending for 3 month(s).",
    },
  ],
  weeklyCheckIn: {
    id: "week-1",
    weekStart: "2026-06-08",
    weekEnd: "2026-06-14",
    status: "GENERATED",
    headline: "This week is about protecting the surplus already visible.",
    wins: ["Income is ahead of posted spending this month.", "Daily spending stayed within run-rate: 4 days"],
    focus: ["Move a calm surplus into savings", "Review recurring subscriptions"],
    generatedAt: "2026-06-11T00:00:00Z",
    completedAt: null,
  },
  milestones: [
    {
      type: "CASHFLOW_WIN",
      title: "Cashflow stayed positive",
      body: "Income is ahead of current posted spending.",
      value: 83000,
      state: "HEALTHY",
    },
  ],
  reminders: [
    {
      type: "SMART_SAVINGS",
      title: "Move a calm surplus into savings",
      body: "Your month-to-date cashflow is positive.",
      actionId: "action-1",
      remindAt: null,
      state: "OPEN",
    },
  ],
  behaviorTimeline: [
    {
      label: "2026-01",
      body: "Net cashflow INR 82000 with INR 38000 spending.",
      occurredOn: "2026-01-01",
      value: 82000,
      state: "HEALTHY",
    },
    {
      label: "2026-02",
      body: "Net cashflow INR 81000 with INR 41000 spending.",
      occurredOn: "2026-02-01",
      value: 81000,
      state: "HEALTHY",
    },
    {
      label: "2026-03",
      body: "Net cashflow INR 83000 with INR 42000 spending.",
      occurredOn: "2026-03-01",
      value: 83000,
      state: "HEALTHY",
    },
  ],
  journey: {
    score: 74,
    state: "HEALTHY",
    headline: "Your journey score blends current savings rate, habit momentum, and completed grounded actions.",
    steps: [
      {
        label: "Awareness",
        state: "HEALTHY",
        progress: 48,
        explanation: "Monthly comparisons are available from imported transactions.",
      },
      {
        label: "Stability",
        state: "HEALTHY",
        progress: 66,
        explanation: "Current month savings rate is measured from posted cashflow.",
      },
      {
        label: "Action",
        state: "STEADY",
        progress: 0,
        explanation: "Completed actions are counted without streak pressure or rewards.",
      },
    ],
  },
};

const notificationPreferencesFixture = {
  inAppEnabled: true,
  budgetWarningsEnabled: true,
  recurringRemindersEnabled: true,
  reportReadyEnabled: true,
  savingsNudgesEnabled: true,
  spendingIncreaseEnabled: true,
  weeklyDigestEnabled: false,
  monthlyReportEnabled: false,
  timezone: "Asia/Kolkata",
  quietHoursStart: null,
  quietHoursEnd: null,
  emailEnabled: true,
  emailAddress: "ci@spendsense.local",
  digestFrequency: "WEEKLY",
  budgetAlertEmailEnabled: true,
  recurringReminderEmailEnabled: true,
  reportEmailEnabled: true,
  deliveryFailureAlertsEnabled: true,
};
