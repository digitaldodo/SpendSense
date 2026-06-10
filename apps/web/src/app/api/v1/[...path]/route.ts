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
  if (route === "/transactions/dashboard-summary") {
    return dashboardFixture;
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
