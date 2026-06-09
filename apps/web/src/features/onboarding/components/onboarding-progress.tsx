"use client";

import { motion } from "framer-motion";

type OnboardingProgressProps = {
  currentStep: number;
  totalSteps: number;
};

export function OnboardingProgress({ currentStep, totalSteps }: OnboardingProgressProps) {
  const percent = ((currentStep + 1) / totalSteps) * 100;

  return (
    <div className="space-y-3" aria-label="Onboarding progress">
      <div className="flex items-center justify-between text-xs font-medium text-muted-foreground">
        <span>Step {currentStep + 1}</span>
        <span>{totalSteps} steps</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-secondary">
        <motion.div
          className="h-full rounded-full bg-primary"
          initial={false}
          animate={{ width: `${percent}%` }}
          transition={{ duration: 0.35, ease: "easeOut" }}
        />
      </div>
    </div>
  );
}
