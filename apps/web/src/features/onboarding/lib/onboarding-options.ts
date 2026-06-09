import {
  type EmploymentType,
  type FinancialGoal,
  type RiskComfort,
  type SalaryRange,
  type SpendingHabit,
} from "@/features/profile/types";

export const salaryRangeOptions: Array<{
  value: SalaryRange;
  label: string;
  description: string;
}> = [
  { value: "UNDER_50K", label: "Under 50k", description: "A lighter monthly income base." },
  { value: "RANGE_50K_100K", label: "50k to 100k", description: "Steady essentials with room to plan." },
  { value: "RANGE_100K_150K", label: "100k to 150k", description: "Growing income and more choices." },
  { value: "RANGE_150K_250K", label: "150k to 250k", description: "Comfortable cashflow to optimize." },
  { value: "OVER_250K", label: "Over 250k", description: "Higher income with richer tradeoffs." },
  { value: "PREFER_NOT_TO_SAY", label: "Prefer not to say", description: "Keep this private for now." },
];

export const employmentTypeOptions: Array<{
  value: EmploymentType;
  label: string;
  description: string;
}> = [
  { value: "SALARIED", label: "Salaried", description: "Predictable monthly income." },
  { value: "SELF_EMPLOYED", label: "Self-employed", description: "Business-led income flow." },
  { value: "FREELANCER", label: "Freelancer", description: "Project-based or variable income." },
  { value: "STUDENT", label: "Student", description: "Learning stage with flexible needs." },
  { value: "HOMEMAKER", label: "Homemaker", description: "Household-led planning rhythm." },
  { value: "RETIRED", label: "Retired", description: "Stability and preservation matter." },
  { value: "BETWEEN_ROLES", label: "Between roles", description: "A transition period worth protecting." },
  { value: "OTHER", label: "Other", description: "A pattern that is more personal." },
];

export const financialGoalOptions: Array<{
  value: FinancialGoal;
  label: string;
  description: string;
}> = [
  { value: "BUILD_EMERGENCY_FUND", label: "Build a cushion", description: "Feel prepared for surprises." },
  { value: "REDUCE_DEBT", label: "Reduce debt", description: "Create a calmer monthly baseline." },
  { value: "SAVE_FOR_HOME", label: "Save for a home", description: "Make a big milestone visible." },
  { value: "PLAN_TRAVEL", label: "Plan travel", description: "Spend joyfully without drift." },
  { value: "INVEST_CONSISTENTLY", label: "Invest consistently", description: "Keep long-term habits steady." },
  { value: "MANAGE_SUBSCRIPTIONS", label: "Manage subscriptions", description: "Catch quiet recurring spend." },
  { value: "UNDERSTAND_SPENDING", label: "Understand spending", description: "See where money actually goes." },
  { value: "PREPARE_FOR_FAMILY", label: "Prepare for family", description: "Plan around people you care for." },
];

export const spendingHabitOptions: Array<{
  value: SpendingHabit;
  label: string;
  description: string;
}> = [
  { value: "TRACKS_EVERYTHING", label: "I track closely", description: "Details help you feel clear." },
  { value: "MOSTLY_PLANNED", label: "Mostly planned", description: "You like a simple structure." },
  { value: "IMPULSE_PURCHASES", label: "Impulse buys happen", description: "You want less friction afterward." },
  { value: "SUBSCRIPTION_HEAVY", label: "Many subscriptions", description: "Recurring spend needs visibility." },
  { value: "CASHFLOW_VARIES", label: "Cashflow varies", description: "Your month needs flexibility." },
  { value: "SHARES_EXPENSES", label: "Shared expenses", description: "Money decisions involve others." },
  { value: "WANTS_GENTLE_NUDGES", label: "Gentle nudges", description: "Support beats pressure." },
];

export const riskComfortOptions: Array<{
  value: RiskComfort;
  label: string;
  description: string;
}> = [
  { value: "LOW", label: "Careful", description: "Protect stability first." },
  { value: "MODERATE", label: "Measured", description: "Small risks with clear reasons." },
  { value: "BALANCED", label: "Balanced", description: "Steady growth without drama." },
  { value: "GROWTH", label: "Growth-minded", description: "Comfortable with some volatility." },
  { value: "HIGH", label: "Bold", description: "Higher risk can feel acceptable." },
];
