"use client";

import { ArrowLeft, TrendingUp } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useFinancialInsights } from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type { MonthlyComparison } from "@/features/finance/types";
import { cn } from "@/lib/utils";

export function CategoryDeepDivePage() {
  const params = useParams<{ categoryId: string }>();
  const insightsQuery = useFinancialInsights();
  const category = insightsQuery.data?.categoryDeepDives.find((item) =>
    params.categoryId === "uncategorized" ? !item.categoryId : item.categoryId === params.categoryId
  );

  if (insightsQuery.isLoading) {
    return (
      <main className="grid gap-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-80 w-full" />
      </main>
    );
  }

  if (!category) {
    return (
      <main className="grid gap-4">
        <Button className="w-fit" variant="ghost" render={<Link href="/insights" />}>
          <ArrowLeft className="size-4" aria-hidden />
          Insights
        </Button>
        <Card className="rounded-lg border-border shadow-raised">
          <CardContent className="grid min-h-52 place-items-center text-sm text-muted-foreground">
            Category report is not available for the current range.
          </CardContent>
        </Card>
      </main>
    );
  }

  return (
    <main className="grid gap-5">
      <section className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
        <Button className="mb-4 w-fit" variant="ghost" render={<Link href="/insights" />}>
          <ArrowLeft className="size-4" aria-hidden />
          Insights
        </Button>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Category deep dive</p>
            <h2 className="mt-1 text-2xl font-semibold sm:text-3xl">{category.categoryName}</h2>
          </div>
          <div className="text-left sm:text-right">
            <p className="text-sm text-muted-foreground">Latest month</p>
            <p className="text-xl font-semibold tabular-nums">{formatMoney(category.latestMonthSpend)}</p>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        <Metric label="Total spend" value={formatMoney(category.totalSpend)} />
        <Metric label="Monthly average" value={formatMoney(category.averageMonthlySpend)} />
        <Metric label="Trend" value={`${Math.round(category.trendPercent)}%`} />
      </section>

      <Card className="rounded-lg border-border shadow-raised">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <TrendingUp className="size-4 text-primary" aria-hidden />
            Monthly movement
          </CardTitle>
        </CardHeader>
        <CardContent>
          <CategoryLine data={category.monthlyValues} />
        </CardContent>
      </Card>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardContent className="p-4">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className="mt-2 truncate text-xl font-semibold tabular-nums">{value}</p>
      </CardContent>
    </Card>
  );
}

function CategoryLine({ data }: { data: MonthlyComparison[] }) {
  if (data.length === 0) {
    return <div className="grid min-h-44 place-items-center text-sm text-muted-foreground">No category trend yet.</div>;
  }
  const values = data.map((item) => item.expense);
  const max = Math.max(...values, 1);
  const points = data.map((item, index) => {
    const x = data.length === 1 ? 50 : (index / (data.length - 1)) * 100;
    const y = 88 - (item.expense / max) * 72;
    return `${x},${y}`;
  });
  return (
    <div className="grid gap-3">
      <svg className="h-56 w-full overflow-visible" role="img" aria-label="Category monthly movement" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={points.join(" ")} stroke="var(--primary)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
        {points.map((point) => {
          const [x, y] = point.split(",");
          return <circle key={point} cx={x} cy={y} fill="var(--background)" r="1.8" stroke="var(--primary)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />;
        })}
      </svg>
      <div className={cn("grid gap-2 text-xs text-muted-foreground", data.length > 4 ? "grid-cols-3 sm:grid-cols-6" : "grid-cols-2")}>
        {data.map((item) => (
          <span key={item.periodStart}>{new Date(item.periodStart).toLocaleDateString("en-IN", { month: "short", year: "2-digit" })}</span>
        ))}
      </div>
    </div>
  );
}
