export const salaryRanges = [
  "UNDER_50K",
  "RANGE_50K_100K",
  "RANGE_100K_150K",
  "RANGE_150K_250K",
  "OVER_250K",
  "PREFER_NOT_TO_SAY",
] as const;

export const employmentTypes = [
  "SALARIED",
  "SELF_EMPLOYED",
  "FREELANCER",
  "STUDENT",
  "HOMEMAKER",
  "RETIRED",
  "BETWEEN_ROLES",
  "OTHER",
] as const;

export const financialGoals = [
  "BUILD_EMERGENCY_FUND",
  "REDUCE_DEBT",
  "SAVE_FOR_HOME",
  "PLAN_TRAVEL",
  "INVEST_CONSISTENTLY",
  "MANAGE_SUBSCRIPTIONS",
  "UNDERSTAND_SPENDING",
  "PREPARE_FOR_FAMILY",
] as const;

export const spendingHabits = [
  "TRACKS_EVERYTHING",
  "MOSTLY_PLANNED",
  "IMPULSE_PURCHASES",
  "SUBSCRIPTION_HEAVY",
  "CASHFLOW_VARIES",
  "SHARES_EXPENSES",
  "WANTS_GENTLE_NUDGES",
] as const;

export const riskComfortLevels = ["LOW", "MODERATE", "BALANCED", "GROWTH", "HIGH"] as const;

export const onboardingSteps = [
  "WELCOME",
  "INCOME_RANGE",
  "EMPLOYMENT_TYPE",
  "MONTHLY_FIXED_EXPENSES",
  "FINANCIAL_GOALS",
  "SPENDING_HABITS",
  "RISK_COMFORT",
  "COMPLETION",
] as const;

export type SalaryRange = (typeof salaryRanges)[number];
export type EmploymentType = (typeof employmentTypes)[number];
export type FinancialGoal = (typeof financialGoals)[number];
export type SpendingHabit = (typeof spendingHabits)[number];
export type RiskComfort = (typeof riskComfortLevels)[number];
export type OnboardingStep = (typeof onboardingSteps)[number];

export type FinancialPreferences = {
  salaryRange: SalaryRange | null;
  employmentType: EmploymentType | null;
  monthlyFixedExpenses: number | null;
  goals: FinancialGoal[];
  spendingHabits: SpendingHabit[];
  riskComfort: RiskComfort | null;
};

export type OnboardingProgress = {
  currentStep: number;
  completedSteps: OnboardingStep[];
  completedAt: string | null;
};

export type Profile = {
  id: string;
  supabaseUserId: string;
  email: string;
  displayName: string | null;
  onboardingCompleted: boolean;
  onboardingCompletedAt: string | null;
  onboardingProgress: OnboardingProgress;
  financialPreferences: FinancialPreferences;
  createdAt: string;
  updatedAt: string;
};

export type OnboardingStatus = {
  completed: boolean;
  currentStep: number;
  completedSteps: OnboardingStep[];
  nextRoute: string;
};

export type OnboardingProgressUpdate = {
  currentStep?: number;
  completedSteps?: OnboardingStep[];
  salaryRange?: SalaryRange;
  employmentType?: EmploymentType;
  monthlyFixedExpenses?: number;
  goals?: FinancialGoal[];
  spendingHabits?: SpendingHabit[];
  riskComfort?: RiskComfort;
};
