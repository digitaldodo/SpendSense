"use client";

import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

type OnboardingOptionCardProps<T extends string> = {
  label: string;
  description: string;
  value: T;
  selected: boolean;
  onSelect: (value: T) => void;
  multi?: boolean;
};

export function OnboardingOptionCard<T extends string>({
  label,
  description,
  value,
  selected,
  onSelect,
  multi,
}: OnboardingOptionCardProps<T>) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={() => onSelect(value)}
      className={cn(
        "group flex min-h-24 w-full items-start justify-between gap-3 rounded-lg border bg-card p-4 text-left transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-raised focus-visible:ring-3 focus-visible:ring-ring/40 focus-visible:outline-none",
        selected
          ? "border-primary/60 bg-primary/7 shadow-raised"
          : "border-border"
      )}
    >
      <span className="grid gap-1">
        <span className="text-sm font-semibold text-foreground">{label}</span>
        <span className="text-sm leading-5 text-muted-foreground">{description}</span>
      </span>
      <span
        className={cn(
          "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-md border transition-colors",
          selected
            ? "border-primary bg-primary text-primary-foreground"
            : "border-border bg-background text-transparent"
        )}
      >
        <Check className="size-3.5" aria-hidden />
      </span>
      <span className="sr-only">{multi ? "Toggle option" : "Select option"}</span>
    </button>
  );
}
