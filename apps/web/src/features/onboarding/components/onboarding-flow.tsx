"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeft, ArrowRight, CheckCircle2, Loader2, Sparkles } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { useForm, useWatch, type FieldPath } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FormError } from "@/components/feedback/form-error";
import {
  employmentTypeOptions,
  financialGoalOptions,
  riskComfortOptions,
  salaryRangeOptions,
  spendingHabitOptions,
} from "@/features/onboarding/lib/onboarding-options";
import {
  onboardingSchema,
  type OnboardingFormValues,
} from "@/features/onboarding/schemas/onboarding-schema";
import { OnboardingOptionCard } from "@/features/onboarding/components/onboarding-option-card";
import { OnboardingProgress } from "@/features/onboarding/components/onboarding-progress";
import {
  useCompleteOnboarding,
  useProfile,
  useSaveOnboardingProgress,
} from "@/features/profile/hooks/use-profile";
import {
  onboardingSteps,
  type FinancialGoal,
  type OnboardingProgressUpdate,
  type OnboardingStep,
  type SpendingHabit,
} from "@/features/profile/types";
import { cn } from "@/lib/utils";

const stepFields: Record<number, Array<FieldPath<OnboardingFormValues>>> = {
  0: [],
  1: ["salaryRange"],
  2: ["employmentType"],
  3: ["monthlyFixedExpenses"],
  4: ["goals"],
  5: ["spendingHabits"],
  6: ["riskComfort"],
  7: [],
};

export function OnboardingFlow() {
  const router = useRouter();
  const profileQuery = useProfile();
  const profile = profileQuery.data;
  const saveProgress = useSaveOnboardingProgress();
  const saveProgressMutate = saveProgress.mutate;
  const completeOnboarding = useCompleteOnboarding();
  const [currentStep, setCurrentStep] = useState(profile?.onboardingProgress.currentStep ?? 0);
  const [completedSteps, setCompletedSteps] = useState<OnboardingStep[]>(
    profile?.onboardingProgress.completedSteps ?? []
  );
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved" | "error">("idle");
  const autosaveTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const autosaveReady = useRef(false);

  const form = useForm<OnboardingFormValues>({
    mode: "onBlur",
    resolver: zodResolver(onboardingSchema),
    defaultValues: {
      salaryRange: profile?.financialPreferences.salaryRange ?? undefined,
      employmentType: profile?.financialPreferences.employmentType ?? undefined,
      monthlyFixedExpenses: profile?.financialPreferences.monthlyFixedExpenses ?? undefined,
      goals: profile?.financialPreferences.goals ?? [],
      spendingHabits: profile?.financialPreferences.spendingHabits ?? [],
      riskComfort: profile?.financialPreferences.riskComfort ?? undefined,
    } as Partial<OnboardingFormValues>,
  });
  const watchedValues = useWatch({ control: form.control });

  useEffect(() => {
    if (!profile) {
      return;
    }

    if (profile.onboardingCompleted) {
      router.replace("/dashboard");
    }
  }, [profile, router]);

  useEffect(() => {
    if (!autosaveReady.current) {
      autosaveReady.current = true;
      return;
    }

    if (autosaveTimer.current) {
      clearTimeout(autosaveTimer.current);
    }

    autosaveTimer.current = setTimeout(() => {
      setSaveState("saving");
      saveProgressMutate(buildPayload(form.getValues(), currentStep, completedSteps), {
        onSuccess: () => setSaveState("saved"),
        onError: () => setSaveState("error"),
      });
    }, 700);

    return () => {
      if (autosaveTimer.current) {
        clearTimeout(autosaveTimer.current);
      }
    };
  }, [completedSteps, currentStep, form, saveProgressMutate, watchedValues]);

  const watchedGoals = watchedValues.goals ?? [];
  const watchedHabits = watchedValues.spendingHabits ?? [];
  const direction = useMemo(() => (currentStep === 0 ? 1 : 0), [currentStep]);

  async function goNext() {
    const isValid = await form.trigger(stepFields[currentStep]);
    if (!isValid) {
      return;
    }

    const nextStep = Math.min(currentStep + 1, onboardingSteps.length - 1);
    const nextCompleted = Array.from(new Set([...completedSteps, onboardingSteps[currentStep]]));
    setCurrentStep(nextStep);
    setCompletedSteps(nextCompleted);
    persist(nextStep, nextCompleted);
  }

  function goBack() {
    const previousStep = Math.max(currentStep - 1, 0);
    setCurrentStep(previousStep);
    persist(previousStep, completedSteps);
  }

  async function finish() {
    const isValid = await form.trigger();
    if (!isValid) {
      setCurrentStep(firstInvalidStep(form.formState.errors));
      return;
    }

    const nextCompleted = Array.from(new Set([...completedSteps, ...onboardingSteps]));
    await saveProgress.mutateAsync(buildPayload(form.getValues(), 7, nextCompleted));
    await completeOnboarding.mutateAsync();
    router.replace("/dashboard");
    router.refresh();
  }

  function persist(step: number, steps: OnboardingStep[]) {
    setSaveState("saving");
    saveProgress.mutate(buildPayload(form.getValues(), step, steps), {
      onSuccess: () => setSaveState("saved"),
      onError: () => setSaveState("error"),
    });
  }

  function toggleGoal(goal: FinancialGoal) {
    const next = watchedGoals.includes(goal)
      ? watchedGoals.filter((item) => item !== goal)
      : [...watchedGoals, goal];
    form.setValue("goals", next, { shouldDirty: true, shouldValidate: true });
  }

  function toggleHabit(habit: SpendingHabit) {
    const next = watchedHabits.includes(habit)
      ? watchedHabits.filter((item) => item !== habit)
      : [...watchedHabits, habit];
    form.setValue("spendingHabits", next, { shouldDirty: true, shouldValidate: true });
  }

  if (!profile || profile.onboardingCompleted) {
    return null;
  }

  return (
    <main className="min-h-screen overflow-hidden bg-[linear-gradient(145deg,var(--background)_0%,color-mix(in_oklch,var(--primary),white_88%)_46%,color-mix(in_oklch,var(--accent),white_82%)_100%)] px-4 py-5 sm:px-6">
      <div className="mx-auto grid min-h-[calc(100vh-2.5rem)] w-full max-w-6xl gap-6 lg:grid-cols-[0.85fr_1.15fr] lg:items-center">
        <aside className="hidden lg:block">
          <div className="max-w-sm space-y-6">
            <div className="inline-flex items-center gap-2 rounded-lg border border-border/70 bg-card/70 px-3 py-1.5 text-sm font-medium text-muted-foreground shadow-raised backdrop-blur">
              <Sparkles className="size-4 text-primary" aria-hidden />
              SpendSense profile
            </div>
            <div className="space-y-4">
              <h1 className="text-4xl font-semibold leading-tight text-foreground">
                Start with the shape of your life, not a spreadsheet.
              </h1>
              <p className="text-base leading-7 text-muted-foreground">
                A few thoughtful answers help SpendSense keep the product calm, relevant, and ready
                for the financial tools coming next.
              </p>
            </div>
          </div>
        </aside>

        <section className="grid content-center gap-5">
          <OnboardingProgress currentStep={currentStep} totalSteps={onboardingSteps.length} />

          <form
            className="overflow-hidden rounded-lg border border-border/70 bg-card/88 shadow-floating backdrop-blur"
            onSubmit={(event) => event.preventDefault()}
          >
            <AnimatePresence mode="wait" custom={direction}>
              <motion.div
                key={currentStep}
                initial={{ opacity: 0, x: 18 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -18 }}
                transition={{ duration: 0.22, ease: "easeOut" }}
                className="grid min-h-[34rem] content-between gap-6 p-5 sm:p-7"
              >
                <div className="space-y-6">{renderStep()}</div>

                <div className="flex flex-col gap-3 border-t border-border pt-4 sm:flex-row sm:items-center sm:justify-between">
                  <div
                    className={cn(
                      "text-xs font-medium",
                      saveState === "error" ? "text-destructive" : "text-muted-foreground"
                    )}
                  >
                    {saveState === "saving" ? "Saving progress" : null}
                    {saveState === "saved" ? "Progress saved" : null}
                    {saveState === "error" ? "Autosave paused. Try again in a moment." : null}
                    {saveState === "idle" ? "Your answers autosave as you go" : null}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      className="h-10 gap-2"
                      onClick={goBack}
                      disabled={currentStep === 0 || saveProgress.isPending || completeOnboarding.isPending}
                    >
                      <ArrowLeft className="size-4" aria-hidden />
                      Back
                    </Button>
                    {currentStep === onboardingSteps.length - 1 ? (
                      <Button
                        type="button"
                        className="h-10 gap-2"
                        onClick={finish}
                        disabled={saveProgress.isPending || completeOnboarding.isPending}
                      >
                        {completeOnboarding.isPending ? (
                          <Loader2 className="size-4 animate-spin" aria-hidden />
                        ) : (
                          <CheckCircle2 className="size-4" aria-hidden />
                        )}
                        Finish
                      </Button>
                    ) : (
                      <Button
                        type="button"
                        className="h-10 gap-2"
                        onClick={goNext}
                        disabled={saveProgress.isPending}
                      >
                        Continue
                        <ArrowRight className="size-4" aria-hidden />
                      </Button>
                    )}
                  </div>
                </div>
              </motion.div>
            </AnimatePresence>
          </form>
        </section>
      </div>
    </main>
  );

  function renderStep() {
    switch (currentStep) {
      case 0:
        return (
          <StepIntro
            eyebrow="Welcome"
            title="What would make money feel calmer this month?"
            description="SpendSense begins with context. No scores, no lectures, no complicated setup."
          >
            <div className="grid gap-3 rounded-lg border border-border bg-muted/40 p-4 text-sm leading-6 text-muted-foreground">
              <p>We will save your progress quietly, so you can leave and continue later.</p>
              <p>Your answers become profile preferences only. Analytics and advice arrive in later phases.</p>
            </div>
          </StepIntro>
        );
      case 1:
        return (
          <StepIntro
            eyebrow="Income range"
            title="Choose a monthly income range."
            description="A range is enough. This helps future experiences match your planning rhythm."
          >
            <OptionGrid>
              {salaryRangeOptions.map((option) => (
                <OnboardingOptionCard
                  key={option.value}
                  {...option}
                  selected={watchedValues.salaryRange === option.value}
                  onSelect={(value) => form.setValue("salaryRange", value, { shouldDirty: true, shouldValidate: true })}
                />
              ))}
            </OptionGrid>
            <FormError message={form.formState.errors.salaryRange?.message} />
          </StepIntro>
        );
      case 2:
        return (
          <StepIntro
            eyebrow="Employment"
            title="What kind of income pattern should we expect?"
            description="This stays flexible. Pick the closest match for today."
          >
            <OptionGrid>
              {employmentTypeOptions.map((option) => (
                <OnboardingOptionCard
                  key={option.value}
                  {...option}
                  selected={watchedValues.employmentType === option.value}
                  onSelect={(value) => form.setValue("employmentType", value, { shouldDirty: true, shouldValidate: true })}
                />
              ))}
            </OptionGrid>
            <FormError message={form.formState.errors.employmentType?.message} />
          </StepIntro>
        );
      case 3:
        return (
          <StepIntro
            eyebrow="Fixed expenses"
            title="About how much is committed each month?"
            description="Think rent, EMIs, utilities, insurance, subscriptions, and other recurring essentials."
          >
            <label className="grid max-w-md gap-2">
              <span className="text-sm font-medium text-foreground">Monthly fixed expenses</span>
              <Input
                className="h-12 text-base"
                type="number"
                min={0}
                step={100}
                inputMode="decimal"
                placeholder="45000"
                {...form.register("monthlyFixedExpenses", { valueAsNumber: true })}
              />
            </label>
            <FormError message={form.formState.errors.monthlyFixedExpenses?.message} />
          </StepIntro>
        );
      case 4:
        return (
          <StepIntro
            eyebrow="Goals"
            title="What are you hoping money helps you protect or create?"
            description="Pick every goal that feels alive right now."
          >
            <OptionGrid>
              {financialGoalOptions.map((option) => (
                <OnboardingOptionCard
                  key={option.value}
                  {...option}
                  selected={watchedGoals.includes(option.value)}
                  onSelect={toggleGoal}
                  multi
                />
              ))}
            </OptionGrid>
            <FormError message={form.formState.errors.goals?.message} />
          </StepIntro>
        );
      case 5:
        return (
          <StepIntro
            eyebrow="Spending habits"
            title="Which patterns sound familiar?"
            description="This is about designing support around real behavior, not perfect behavior."
          >
            <OptionGrid>
              {spendingHabitOptions.map((option) => (
                <OnboardingOptionCard
                  key={option.value}
                  {...option}
                  selected={watchedHabits.includes(option.value)}
                  onSelect={toggleHabit}
                  multi
                />
              ))}
            </OptionGrid>
            <FormError message={form.formState.errors.spendingHabits?.message} />
          </StepIntro>
        );
      case 6:
        return (
          <StepIntro
            eyebrow="Risk comfort"
            title="How much uncertainty feels acceptable?"
            description="There is no best answer. This helps future planning stay emotionally honest."
          >
            <div className="grid gap-3">
              {riskComfortOptions.map((option) => (
                <OnboardingOptionCard
                  key={option.value}
                  {...option}
                  selected={watchedValues.riskComfort === option.value}
                  onSelect={(value) => form.setValue("riskComfort", value, { shouldDirty: true, shouldValidate: true })}
                />
              ))}
            </div>
            <FormError message={form.formState.errors.riskComfort?.message} />
          </StepIntro>
        );
      default:
        return (
          <StepIntro
            eyebrow="Ready"
            title="Your profile foundation is set."
            description="Next you will enter a quiet dashboard shell. The heavier financial tools are still intentionally out of scope."
          >
            <div className="grid gap-3 rounded-lg border border-border bg-muted/40 p-4 text-sm text-muted-foreground">
              <SummaryRow label="Income" value={labelFor(salaryRangeOptions, form.getValues("salaryRange"))} />
              <SummaryRow label="Employment" value={labelFor(employmentTypeOptions, form.getValues("employmentType"))} />
              <SummaryRow label="Goals" value={`${watchedGoals.length || 0} selected`} />
              <SummaryRow label="Habits" value={`${watchedHabits.length || 0} selected`} />
            </div>
          </StepIntro>
        );
    }
  }
}

function StepIntro({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-5">
      <div className="space-y-2">
        <p className="text-xs font-semibold uppercase tracking-normal text-primary">{eyebrow}</p>
        <h2 className="text-2xl font-semibold leading-tight text-foreground sm:text-3xl">{title}</h2>
        <p className="max-w-2xl text-sm leading-6 text-muted-foreground sm:text-base">{description}</p>
      </div>
      {children}
    </div>
  );
}

function OptionGrid({ children }: { children: React.ReactNode }) {
  return <div className="grid gap-3 sm:grid-cols-2">{children}</div>;
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span>{label}</span>
      <span className="font-medium text-foreground">{value}</span>
    </div>
  );
}

function buildPayload(
  values: Partial<OnboardingFormValues>,
  currentStep: number,
  completedSteps: OnboardingStep[]
): OnboardingProgressUpdate {
  return {
    currentStep,
    completedSteps,
    salaryRange: values.salaryRange,
    employmentType: values.employmentType,
    monthlyFixedExpenses:
      typeof values.monthlyFixedExpenses === "number" && Number.isFinite(values.monthlyFixedExpenses)
        ? values.monthlyFixedExpenses
        : undefined,
    goals: values.goals,
    spendingHabits: values.spendingHabits,
    riskComfort: values.riskComfort,
  };
}

function labelFor<T extends string>(options: Array<{ value: T; label: string }>, value?: T) {
  return options.find((option) => option.value === value)?.label ?? "Not set";
}

function firstInvalidStep(errors: Record<string, unknown>) {
  if (errors.salaryRange) {
    return 1;
  }
  if (errors.employmentType) {
    return 2;
  }
  if (errors.monthlyFixedExpenses) {
    return 3;
  }
  if (errors.goals) {
    return 4;
  }
  if (errors.spendingHabits) {
    return 5;
  }
  if (errors.riskComfort) {
    return 6;
  }
  return 1;
}
