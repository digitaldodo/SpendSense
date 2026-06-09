import { z } from "zod";
import {
  employmentTypes,
  financialGoals,
  riskComfortLevels,
  salaryRanges,
  spendingHabits,
} from "@/features/profile/types";

export const onboardingSchema = z.object({
  salaryRange: z.enum(salaryRanges, {
    error: "Choose the range that feels closest right now.",
  }),
  employmentType: z.enum(employmentTypes, {
    error: "Choose the work pattern that best fits you.",
  }),
  monthlyFixedExpenses: z
    .number({ error: "Enter your monthly fixed expenses." })
    .min(0, "Monthly fixed expenses cannot be negative.")
    .max(100000000, "Enter a realistic monthly amount."),
  goals: z.array(z.enum(financialGoals)).min(1, "Pick at least one goal."),
  spendingHabits: z.array(z.enum(spendingHabits)).min(1, "Pick at least one habit."),
  riskComfort: z.enum(riskComfortLevels, {
    error: "Choose the level that feels most comfortable.",
  }),
});

export type OnboardingFormValues = z.infer<typeof onboardingSchema>;
