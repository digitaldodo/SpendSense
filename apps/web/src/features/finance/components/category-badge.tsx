import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { Category } from "@/features/finance/types";

const colorClasses: Record<string, string> = {
  mint: "border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200",
  blue: "border-sky-200 bg-sky-50 text-sky-800 dark:border-sky-900 dark:bg-sky-950/40 dark:text-sky-200",
  amber: "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-200",
  green: "border-green-200 bg-green-50 text-green-800 dark:border-green-900 dark:bg-green-950/40 dark:text-green-200",
  slate: "border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-800 dark:bg-slate-900/50 dark:text-slate-200",
  rose: "border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-900 dark:bg-rose-950/40 dark:text-rose-200",
  teal: "border-teal-200 bg-teal-50 text-teal-800 dark:border-teal-900 dark:bg-teal-950/40 dark:text-teal-200",
  neutral: "border-border bg-muted text-muted-foreground",
};

export function CategoryBadge({ category, className }: { category?: Category | null; className?: string }) {
  const token = category?.colorToken ?? "neutral";

  return (
    <Badge
      variant="outline"
      className={cn(
        "h-6 rounded-md px-2 text-[0.72rem] font-medium",
        colorClasses[token] ?? colorClasses.neutral,
        className
      )}
    >
      {category?.name ?? "Uncategorized"}
    </Badge>
  );
}
