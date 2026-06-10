import { expect, type Page, type Route, test } from "@playwright/test";

type MockOptions = {
  onboardingCompleted?: boolean;
};

const userProfile = (options: MockOptions = {}) => ({
  id: "20000000-0000-4000-8000-000000000001",
  email: "demo@spendsense.local",
  displayName: "Demo",
  roles: ["USER", "ADMIN"],
  onboardingCompleted: options.onboardingCompleted ?? true,
  onboardingProgress: {
    currentStep: 0,
    completedSteps: [] as string[],
  },
  financialPreferences: {
    salaryRange: null,
    employmentType: null,
    monthlyFixedExpenses: null,
    goals: [],
    spendingHabits: [],
    riskComfort: null,
  },
});

const dashboardSummary = {
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
      originalFilename: "icici-june.csv",
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

const smartActionDashboard = {
  generatedAt: "2026-06-11T00:00:00Z",
  dailySummary: {
    headline: "You are protecting positive cashflow.",
    monthIncome: 125000,
    monthSpend: 42000,
    netCashflow: 83000,
    savingsRate: 66,
    tone: "SUPPORTIVE",
    explanation: "Uses current month posted income minus posted debits.",
  },
  todayFocus: {
    title: "Move a calm surplus into savings",
    body: "Your month-to-date cashflow is positive.",
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
      body: "Moving ₹5,000 keeps the action grounded in actual surplus.",
      explanation: "Calculated as 25% of current positive net cashflow, capped at INR 5000.",
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
      explanation: "4 day(s) in a row stayed at or below the deterministic daily run-rate.",
    },
  ],
  weeklyCheckIn: {
    id: "week-1",
    weekStart: "2026-06-08",
    weekEnd: "2026-06-14",
    status: "GENERATED",
    headline: "This week is about protecting the surplus already visible.",
    wins: ["Income is ahead of posted spending this month."],
    focus: ["Move a calm surplus into savings"],
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
      label: "2026-06",
      body: "Net cashflow ₹83,000 with ₹42,000 spending.",
      occurredOn: "2026-06-01",
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
        explanation: "Monthly comparisons are available.",
      },
    ],
  },
};
type MockSmartAction = Omit<
  (typeof smartActionDashboard.actions)[number],
  "completedAt" | "status"
> & {
  completedAt: string | null;
  status: string;
};
type MockWeeklyCheckIn = Omit<typeof smartActionDashboard.weeklyCheckIn, "completedAt" | "status"> & {
  completedAt: string | null;
  status: string;
};
type MockSmartActionDashboard = Omit<
  typeof smartActionDashboard,
  "actions" | "weeklyCheckIn"
> & {
  actions: MockSmartAction[];
  weeklyCheckIn: MockWeeklyCheckIn;
};

test.beforeEach(async ({ page }) => {
  await page.context().addCookies([
    {
      name: "__spendsense_e2e_session",
      value: "1",
      domain: "127.0.0.1",
      path: "/",
      sameSite: "Lax",
    },
  ]);
  await page.addInitScript(() => {
    window.localStorage.setItem(
      "__SPENDSENSE_E2E_SESSION__",
      JSON.stringify({
        access_token: "e2e-token",
        refresh_token: "e2e-refresh",
        token_type: "bearer",
        expires_in: 3600,
        expires_at: Math.floor(Date.now() / 1000) + 3600,
        user: {
          id: "20000000-0000-4000-8000-000000000001",
          email: "demo@spendsense.local",
          app_metadata: {},
          user_metadata: {},
          aud: "authenticated",
          created_at: new Date().toISOString(),
        },
      })
    );
  });
});

test("authenticated dashboard renders financial data and accessible chart labels", async ({
  page,
}) => {
  await mockApi(page);

  await page.goto("/dashboard");

  await expect(page.getByRole("heading", { name: /Your money view/i })).toBeVisible();
  await expect(page.getByText("Account balance")).toBeVisible();
  await expect(page.getByText("Daily financial summary")).toBeVisible();
  await expect(page.getByText("Smart action center")).toBeVisible();
  await page.getByRole("button", { name: "Done" }).first().click();
  await expect(page.locator("#smart-actions").getByText("Complete", { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("img", { name: "Monthly spending trend" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Notifications" })).toBeVisible();
  await page.keyboard.press("Tab");
  await expect(page.getByRole("link", { name: "Skip to content" })).toBeFocused();
});

test("onboarding flow saves progress and completes", async ({ page }) => {
  await mockApi(page, { onboardingCompleted: false });

  await page.goto("/onboarding");

  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByRole("button", { name: /50k to 100k/i }).click();
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByText("Salaried").click();
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByPlaceholder("45000").fill("42000");
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByRole("button", { name: /Build a cushion/i }).click();
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByRole("button", { name: /Impulse buys happen/i }).click();
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByRole("button", { name: /Balanced/i }).click();
  await page.getByRole("button", { name: /Continue/i }).click();
  await page.getByRole("button", { name: /Finish/i }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});

test("csv import validates upload, previews rows, and confirms import", async ({ page }) => {
  await mockApi(page);

  await page.goto("/imports");
  await page.locator("input[type='file']").setInputFiles({
    name: "transactions.csv",
    mimeType: "text/csv",
    buffer: Buffer.from("date,amount,merchant\n2026-06-01,499,Cafe\n"),
  });

  await expect(page.getByText("Column mapping")).toBeVisible();
  await expect(page.getByText("Cafe")).toBeVisible();
  await page.getByRole("button", { name: /Confirm import/i }).click();
  await expect(page.getByText("Import complete")).toBeVisible();
});

test("notification flow marks messages read and shows delivery status", async ({ page }) => {
  await mockApi(page);

  await page.goto("/notifications");
  await expect(page.getByText("Budget nearing limit")).toBeVisible();
  await page.getByTitle("Mark read").click();
  await expect(page.getByText("Delivery system")).toBeVisible();
  await page.getByRole("button", { name: /Delivery/i }).click();
  await expect(page.getByText("Delivery history")).toBeVisible();
});

test("ai mentor explains spending with grounded cards", async ({ page }) => {
  await mockApi(page);

  await page.goto("/mentor");

  await expect(page.getByRole("heading", { name: "Financial mentor" })).toBeVisible();
  await page.getByRole("button", { name: "Ask Why did I overspend this month?" }).click();
  await expect(page.getByText("Food is above its recent baseline")).toBeVisible();
  await expect(page.getByText("Grounded in your ledger")).toBeVisible();
  await expect(page.getByRole("button", { name: "What changed compared to last month?" })).toBeVisible();
});

test("mobile layout keeps primary navigation reachable", async ({ page, isMobile }) => {
  test.skip(!isMobile, "mobile-only responsive check");
  await mockApi(page);

  await page.goto("/dashboard");

  await expect(page.getByLabel("Mobile primary")).toBeVisible();
  await page.getByRole("link", { name: /Import/i }).click();
  await expect(page).toHaveURL(/\/imports/);
  await expect(page.getByText("Secure CSV ingestion")).toBeVisible();
});

async function mockApi(page: Page, options: MockOptions = {}) {
  let profile = userProfile(options);
  let actionsDashboard: MockSmartActionDashboard = structuredClone(smartActionDashboard);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;

    if (request.method() === "OPTIONS") {
      return route.fulfill({
        status: 204,
        headers: corsHeaders(),
      });
    }

    if (path === "/api/v1/profile/current") {
      return json(route, profile);
    }
    if (path === "/api/v1/onboarding/progress") {
      const body = request.postDataJSON() as { currentStep?: number; completedSteps?: string[] };
      profile = {
        ...profile,
        onboardingProgress: {
          currentStep: body.currentStep ?? 0,
          completedSteps: body.completedSteps ?? [],
        },
      };
      return json(route, profile);
    }
    if (path === "/api/v1/onboarding/complete") {
      profile = { ...profile, onboardingCompleted: true };
      return json(route, profile);
    }
    if (path === "/api/v1/transactions/dashboard-summary") {
      return json(route, dashboardSummary);
    }
    if (path === "/api/v1/actions/dashboard") {
      return json(route, actionsDashboard);
    }
    if (path === "/api/v1/actions/action-1/complete") {
      actionsDashboard = {
        ...actionsDashboard,
        actions: actionsDashboard.actions.map((action) =>
          action.id === "action-1"
            ? { ...action, status: "COMPLETED", completedAt: "2026-06-11T00:01:00Z" }
            : action
        ),
      };
      return json(route, actionsDashboard.actions[0]);
    }
    if (path === "/api/v1/actions/action-1/dismiss" || path === "/api/v1/actions/action-1/snooze") {
      return json(route, actionsDashboard.actions[0]);
    }
    if (path === "/api/v1/actions/weekly-check-in/complete") {
      actionsDashboard = {
        ...actionsDashboard,
        weeklyCheckIn: {
          ...actionsDashboard.weeklyCheckIn,
          status: "COMPLETED",
          completedAt: "2026-06-11T00:02:00Z",
        },
      };
      return json(route, actionsDashboard.weeklyCheckIn);
    }
    if (path === "/api/v1/accounts") {
      return json(route, dashboardSummary.accounts);
    }
    if (path === "/api/v1/categories") {
      return json(route, [
        {
          id: "cat-food",
          name: "Food",
          colorToken: "green",
          iconName: "tag",
          systemCategory: true,
        },
      ]);
    }
    if (path === "/api/v1/transactions") {
      return json(route, { items: [], totalItems: 0, page: 0, size: 12, hasNext: false });
    }
    if (
      path === "/api/v1/planning/budgets" ||
      path === "/api/v1/planning/budgets/history" ||
      path === "/api/v1/planning/goals"
    ) {
      return json(route, []);
    }
    if (path === "/api/v1/imports/csv/preview") {
      return json(route, {
        filename: "transactions.csv",
        fileChecksum: "checksum-phase-13",
        fileSignature: "date|amount|merchant",
        columns: ["date", "amount", "merchant"],
        mapping: { date: "date", amount: "amount", merchant: "merchant" },
        mappingConfidenceScore: 96,
        reusedMapping: null,
        recordsSeen: 1,
        validRows: 1,
        failedRows: 0,
        duplicateRows: 0,
        previewRows: [
          {
            rowNumber: 1,
            raw: { date: "2026-06-01", amount: "499", merchant: "Cafe" },
            occurredAt: "2026-06-01T00:00:00Z",
            amount: 499,
            direction: "DEBIT",
            merchantName: "Cafe",
            description: "Cafe",
            reference: "CSV/1",
            duplicate: false,
            warning: null,
          },
        ],
        failures: [],
      });
    }
    if (path === "/api/v1/imports/csv") {
      return json(route, {
        job: {
          id: "job-1",
          originalFilename: "transactions.csv",
          status: "COMPLETED",
          recordsSeen: 1,
          recordsImported: 1,
          recordsDuplicate: 0,
          recordsFailed: 0,
          startedAt: "2026-06-10T00:00:00Z",
          completedAt: "2026-06-10T00:00:01Z",
        },
        recordsSeen: 1,
        recordsImported: 1,
        recordsDuplicate: 0,
        recordsFailed: 0,
        failures: [],
      });
    }
    if (path === "/api/v1/imports/mappings") {
      return json(route, []);
    }
    if (path === "/api/v1/notifications/summary") {
      return json(route, { unreadCount: 1, activeCount: 1, latest: [], timeline: [] });
    }
    if (path === "/api/v1/notifications") {
      return json(route, [
        {
          id: "notice-1",
          type: "BUDGET_THRESHOLD",
          severity: "CAUTION",
          title: "Budget nearing limit",
          body: "Food spending is close to the planned threshold.",
          actionLabel: "Review",
          actionUrl: "/dashboard",
          read: false,
          readAt: null,
          createdAt: "2026-06-10T08:00:00Z",
        },
      ]);
    }
    if (
      path === "/api/v1/notifications/notice-1/read" ||
      path === "/api/v1/notifications/read-all"
    ) {
      return json(route, null);
    }
    if (path === "/api/v1/notifications/system-status") {
      return json(route, {
        status: "HEALTHY",
        deliverySuccessRate: 99,
        pendingRetries: 0,
        lastWorkerHeartbeatAt: "2026-06-10T08:00:00Z",
      });
    }
    if (path === "/api/v1/notifications/preferences") {
      return json(route, {
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
        emailAddress: "demo@spendsense.local",
        digestFrequency: "WEEKLY",
        budgetAlertEmailEnabled: true,
        recurringReminderEmailEnabled: true,
        reportEmailEnabled: true,
        deliveryFailureAlertsEnabled: true,
      });
    }
    if (
      path === "/api/v1/notifications/scheduled-reports" ||
      path === "/api/v1/notifications/scheduled-reports/delivery-logs"
    ) {
      return json(route, []);
    }
    if (path === "/api/v1/notifications/deliveries") {
      return json(route, [
        {
          id: "delivery-1",
          deliveryKind: "WEEKLY_SUMMARY",
          subject: "Weekly summary",
          channel: "EMAIL",
          provider: "RESEND",
          recipient: "demo@spendsense.local",
          status: "DELIVERED",
          attemptCount: 1,
          createdAt: "2026-06-10T08:00:00Z",
          nextRetryAt: null,
          errorMessage: null,
        },
      ]);
    }
    if (path === "/api/v1/notifications/worker-jobs") {
      return json(route, []);
    }
    if (path === "/api/v1/notifications/email-preview") {
      return json(route, { subject: "SpendSense summary", html: "<p>Ready</p>", text: "Ready" });
    }
    if (path === "/api/v1/ai/conversations" && request.method() === "GET") {
      return json(route, []);
    }
    if (path === "/api/v1/ai/insights/timeline") {
      return json(route, [
        {
          type: "CASHFLOW",
          state: "HEALTHY",
          title: "Current cashflow",
          body: "Income minus posted spending for the current month.",
          primaryValue: 83000,
          comparisonValue: 81000,
          actionLabel: "Explain change",
          actionIntent: "MONTHLY_CHANGE",
        },
      ]);
    }
    if (path === "/api/v1/ai/conversations/messages" && request.method() === "POST") {
      return json(route, {
        conversation: {
          id: "ai-conversation-1",
          title: "Why did I overspend this month?",
          status: "ACTIVE",
          contextScope: "FINANCIAL_WORKSPACE",
          lastMessageAt: "2026-06-10T08:00:00Z",
          createdAt: "2026-06-10T08:00:00Z",
          lastMessagePreview: "Food is above its recent baseline.",
        },
        userMessage: {
          id: "ai-user-message-1",
          conversationId: "ai-conversation-1",
          role: "USER",
          intent: "OVERSPEND_EXPLANATION",
          content: "Why did I overspend this month?",
          insightCards: [],
          followUpPrompts: [],
          safetyFlags: [],
          promptTokens: 0,
          completionTokens: 0,
          latencyMs: 0,
          createdAt: "2026-06-10T08:00:00Z",
        },
        assistantMessage: {
          id: "ai-assistant-message-1",
          conversationId: "ai-conversation-1",
          role: "ASSISTANT",
          intent: "OVERSPEND_EXPLANATION",
          content:
            "Food is above its recent baseline by INR 2,000. This answer uses posted transactions and active budget summaries only.",
          insightCards: [
            {
              type: "CATEGORY_IMPACT",
              state: "CAUTION",
              title: "Food",
              body: "Largest current category pressure in the compact context.",
              primaryValue: 18000,
              comparisonValue: 16000,
              actionLabel: "Explain category",
              actionIntent: "CATEGORY_SAVINGS_IMPACT",
            },
          ],
          followUpPrompts: [
            "What changed compared to last month?",
            "Which transaction should I inspect first?",
          ],
          safetyFlags: [],
          provider: "LOCAL_DETERMINISTIC",
          model: "spendsense-mentor-v1-local",
          promptTokens: 42,
          completionTokens: 58,
          latencyMs: 12,
          createdAt: "2026-06-10T08:00:01Z",
        },
        insightCards: [
          {
            type: "CATEGORY_IMPACT",
            state: "CAUTION",
            title: "Food",
            body: "Largest current category pressure in the compact context.",
            primaryValue: 18000,
            comparisonValue: 16000,
            actionLabel: "Explain category",
            actionIntent: "CATEGORY_SAVINGS_IMPACT",
          },
        ],
        followUpPrompts: [
          "What changed compared to last month?",
          "Which transaction should I inspect first?",
        ],
        usage: {
          provider: "LOCAL_DETERMINISTIC",
          model: "spendsense-mentor-v1-local",
          promptTokens: 42,
          completionTokens: 58,
          totalTokens: 100,
          estimatedCostMinor: 0,
          currency: "INR",
          latencyMs: 12,
        },
        grounded: true,
        safetyLevel: "CLEAR",
        citations: ["monthly summaries", "category trends"],
      });
    }
    return json(route, null);
  });
}

async function json(route: Route, data: unknown) {
  return route.fulfill({
    status: 200,
    headers: {
      ...corsHeaders(),
      "content-type": "application/json",
    },
    body: JSON.stringify({ success: true, data, message: "ok", traceId: "e2e-trace" }),
  });
}

function corsHeaders() {
  return {
    "access-control-allow-origin": "http://127.0.0.1:3000",
    "access-control-allow-headers": "authorization,content-type,x-correlation-id",
    "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
    "access-control-allow-credentials": "true",
    vary: "origin",
  };
}
