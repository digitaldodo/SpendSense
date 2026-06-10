"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { motion } from "framer-motion";
import {
  ArrowDownRight,
  ArrowUpRight,
  Calculator,
  CalendarDays,
  Download,
  FileText,
  Gauge,
  Loader2,
  Printer,
  RefreshCcw,
  Repeat2,
  SearchCheck,
  ShieldCheck,
  TrendingUp,
} from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAffordabilityScenario,
  useFinancialHealthBreakdown,
  useFinancialInsights,
  useMaterializeBudgetRollovers,
  useMonthlyReport,
  useProjection,
  useSavingsGoals,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import { downloadReportExport } from "@/features/finance/services/finance-api";
import type {
  AffordabilityScenario,
  BudgetRollover,
  CashflowImpactPoint,
  CategoryDeepDive,
  CategoryTrendInsight,
  DeterministicInsight,
  FinancialHealthBreakdown,
  FinancialProjection,
  MonthlyComparison,
  ProjectionPoint,
  RecurringPattern,
  SavingsTrajectory,
  SpendingAnomaly,
} from "@/features/finance/types";
import { cn } from "@/lib/utils";

export function InsightsPage() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [exporting, setExporting] = useState<string | null>(null);
  const filters = useMemo(() => ({ from: from || undefined, to: to || undefined }), [from, to]);
  const insightsQuery = useFinancialInsights(filters);
  const month = to ? to.slice(0, 7) : undefined;
  const reportQuery = useMonthlyReport(month);
  const rollovers = useMaterializeBudgetRollovers();
  const insights = insightsQuery.data;

  async function exportFile(kind: "monthly-summary" | "category-report" | "pdf") {
    setExporting(kind);
    try {
      const query = new URLSearchParams();
      if (kind === "pdf") {
        if (month) {
          query.set("month", month);
        }
      } else {
        if (from) {
          query.set("from", from);
        }
        if (to) {
          query.set("to", to);
        }
        query.set("type", kind);
      }
      const exportPath = kind === "pdf" ? `/api/v1/reports/exports/pdf?${query}` : `/api/v1/reports/exports/csv?${query}`;
      const file = await downloadReportExport(exportPath);
      const url = URL.createObjectURL(file.blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = file.filename;
      anchor.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(null);
    }
  }

  if (insightsQuery.isLoading) {
    return <InsightsLoading />;
  }

  if (!insights) {
    return (
      <main className="grid gap-4">
        <Card className="rounded-lg border-border shadow-raised">
          <CardContent className="grid min-h-52 place-items-center text-center text-sm text-muted-foreground">
            Reports will appear after transactions are available.
          </CardContent>
        </Card>
      </main>
    );
  }

  return (
    <main className="grid gap-5">
      <section className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">Financial insights</p>
              <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">Deterministic visibility from your ledger.</h2>
              <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
                {insights.periodLabel}. Last generated {new Date(insights.generatedAt).toLocaleString("en-IN")}.
              </p>
            </div>
            <Button variant="outline" onClick={() => window.print()}>
              <Printer className="size-4" aria-hidden />
              Print
            </Button>
          </div>
        </div>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <CalendarDays className="size-4 text-primary" aria-hidden />
              Date comparison
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-[1fr_1fr_auto]">
            <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
            <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
            <Button variant="ghost" onClick={() => { setFrom(""); setTo(""); }}>
              Reset
            </Button>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricTile label="Income" value={formatMoney(insights.summary.income)} tone="success" />
        <MetricTile label="Expenses" value={formatMoney(insights.summary.expense)} tone="primary" />
        <MetricTile label="Net cashflow" value={formatMoney(insights.summary.netCashflow)} tone={insights.summary.netCashflow >= 0 ? "success" : "warning"} />
        <MetricTile label="Recurring spend" value={formatMoney(insights.summary.recurringSpend)} tone="info" />
      </section>

      <ExportPanel
        exporting={exporting}
        onExport={exportFile}
        onPrint={() => window.print()}
        reportGeneratedAt={reportQuery.data?.generatedAt}
        onMaterialize={() => rollovers.mutate()}
        materializing={rollovers.isPending}
        rollovers={rollovers.data}
      />

      <FinancialGuidanceSuite />

      <section className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Insight cards</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            {insights.insights.map((insight, index) => (
              <InsightCard key={`${insight.type}-${index}`} insight={insight} index={index} />
            ))}
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Savings momentum</CardTitle>
          </CardHeader>
          <CardContent>
            <SavingsChart data={insights.savingsTrajectory} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1fr_1fr]">
        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Income vs expense</CardTitle>
          </CardHeader>
          <CardContent>
            <IncomeExpenseChart data={insights.monthlyComparisons} />
          </CardContent>
        </Card>

        <Card className="rounded-lg border-border shadow-raised">
          <CardHeader>
            <CardTitle className="text-base">Financial trend timeline</CardTitle>
          </CardHeader>
          <CardContent>
            <Timeline data={insights.monthlyComparisons} />
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <PatternPanel title="Recurring expense detection" items={insights.recurringTransactions} />
        <PatternPanel title="Subscription tracking" items={insights.subscriptions} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
        <AnomalyPanel anomalies={insights.anomalies} />
        <CategoryTrendPanel trends={insights.categoryTrends} />
      </section>

      <CategoryDeepDiveGrid categories={insights.categoryDeepDives} />

      <section className="print-report rounded-lg border border-border bg-card p-5 shadow-raised sm:p-6">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">Printable summary</h2>
            <p className="mt-1 text-sm text-muted-foreground">{insights.periodLabel}</p>
          </div>
          <FileText className="size-5 text-primary" aria-hidden />
        </div>
        <div className="mt-4 grid gap-2 text-sm">
          <SummaryLine label="Savings rate" value={`${Math.round(insights.summary.savingsRate)}%`} />
          <SummaryLine label="Income stability" value={stateLabel(insights.incomeStability.state)} />
          <SummaryLine label="Spending anomalies" value={`${insights.anomalies.length}`} />
          <SummaryLine label="Subscriptions" value={`${insights.subscriptions.length}`} />
        </div>
      </section>
    </main>
  );
}

function ExportPanel({
  exporting,
  reportGeneratedAt,
  onExport,
  onPrint,
  onMaterialize,
  materializing,
  rollovers,
}: {
  exporting: string | null;
  reportGeneratedAt?: string;
  onExport: (kind: "monthly-summary" | "category-report" | "pdf") => void;
  onPrint: () => void;
  onMaterialize: () => void;
  materializing: boolean;
  rollovers?: BudgetRollover[];
}) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardContent className="grid gap-3 p-4 sm:grid-cols-[1fr_auto] sm:items-center">
        <div className="min-w-0">
          <p className="text-sm font-medium">Monthly financial reports</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {reportGeneratedAt ? `Report refreshed ${new Date(reportGeneratedAt).toLocaleString("en-IN")}` : "Report aggregation is ready."}
            {rollovers?.length ? ` ${rollovers.length} rollover(s) materialized.` : ""}
          </p>
        </div>
        <div className="grid grid-cols-2 gap-2 sm:flex">
          <ExportButton label="CSV" loading={exporting === "monthly-summary"} onClick={() => onExport("monthly-summary")} />
          <ExportButton label="Category" loading={exporting === "category-report"} onClick={() => onExport("category-report")} />
          <ExportButton label="PDF" loading={exporting === "pdf"} onClick={() => onExport("pdf")} />
          <Button variant="outline" onClick={onPrint}>
            <Printer className="size-4" aria-hidden />
            Print
          </Button>
          <Button variant="ghost" disabled={materializing} onClick={onMaterialize}>
            {materializing ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <RefreshCcw className="size-4" aria-hidden />}
            Rollovers
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function ExportButton({ label, loading, onClick }: { label: string; loading: boolean; onClick: () => void }) {
  return (
    <Button variant="outline" disabled={loading} onClick={onClick}>
      {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <Download className="size-4" aria-hidden />}
      {label}
    </Button>
  );
}

function FinancialGuidanceSuite() {
  const healthQuery = useFinancialHealthBreakdown();
  const goalsQuery = useSavingsGoals();
  const affordability = useAffordabilityScenario();
  const projection = useProjection();
  const projectionStartedRef = useRef(false);
  const goals = goalsQuery.data ?? [];

  useEffect(() => {
    if (!projectionStartedRef.current && !projection.data && !projection.isPending) {
      projectionStartedRef.current = true;
      projection.mutate({ months: 36 });
    }
  }, [projection]);

  return (
    <section className="grid gap-4">
      <div>
        <h2 className="text-xl font-semibold">Financial guidance lab</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Deterministic health signals, EMI impact, and future balance visibility.
        </p>
      </div>

      <section className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <FinancialHealthBreakdownPanel health={healthQuery.data} loading={healthQuery.isLoading} />
        <AffordabilitySimulator goals={goals} result={affordability.data} loading={affordability.isPending} onSubmit={(payload) => affordability.mutate(payload)} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <ProjectionPanel projection={projection.data} loading={projection.isPending} onSubmit={(payload) => projection.mutate(payload)} />
        <GuidancePrinciples />
      </section>
    </section>
  );
}

function FinancialHealthBreakdownPanel({
  health,
  loading,
}: {
  health?: FinancialHealthBreakdown;
  loading: boolean;
}) {
  if (loading) {
    return <Skeleton className="h-96 w-full" />;
  }

  if (!health) {
    return (
      <Card className="rounded-lg border-border shadow-raised">
        <CardContent className="grid min-h-72 place-items-center text-center text-sm text-muted-foreground">
          Health guidance appears after transactions are available.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <CardTitle className="flex items-center gap-2 text-base">
            <Gauge className="size-4 text-primary" aria-hidden />
            Health breakdown
          </CardTitle>
          <p className="mt-1 text-sm leading-6 text-muted-foreground">{health.headline}</p>
        </div>
        <span className={cn("w-fit rounded-lg px-2 py-1 text-xs font-medium", stateBadgeClass(health.state))}>
          {stateLabel(health.state)} · {health.score}
        </span>
      </CardHeader>
      <CardContent className="grid gap-4">
        <HealthTrendChart data={health.trendHistory} />
        <div className="grid gap-3 sm:grid-cols-2">
          {health.indicators.map((indicator) => (
            <div key={indicator.key} className="grid gap-2 rounded-lg border border-border/70 bg-background/70 p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="truncate text-sm font-semibold">{indicator.label}</p>
                <span className={cn("rounded-lg px-2 py-1 text-xs font-medium", stateBadgeClass(indicator.state))}>
                  {Math.round(indicator.value)}%
                </span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-muted">
                <div
                  className={cn("h-full rounded-full transition-[width] duration-500", stateFillClass(indicator.state))}
                  style={{ width: `${Math.min(100, Math.max(4, indicator.value))}%` }}
                />
              </div>
              <p className="text-xs leading-5 text-muted-foreground">{indicator.explanation}</p>
              <p className="text-xs font-medium text-foreground">{indicator.actionHint}</p>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

function AffordabilitySimulator({
  goals,
  result,
  loading,
  onSubmit,
}: {
  goals: { id: string; name: string }[];
  result?: AffordabilityScenario;
  loading: boolean;
  onSubmit: (payload: {
    purchaseAmount: number;
    downPayment: number;
    annualInterestRate: number;
    tenureMonths: number;
    existingMonthlyEmis: number;
    goalId?: string;
    currency: string;
  }) => void;
}) {
  const [purchaseAmount, setPurchaseAmount] = useState("90000");
  const [downPayment, setDownPayment] = useState("15000");
  const [rate, setRate] = useState("11");
  const [tenure, setTenure] = useState("12");
  const [existingEmis, setExistingEmis] = useState("0");
  const [goalId, setGoalId] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit({
      purchaseAmount: Number(purchaseAmount),
      downPayment: Number(downPayment || 0),
      annualInterestRate: Number(rate || 0),
      tenureMonths: Number(tenure || 1),
      existingMonthlyEmis: Number(existingEmis || 0),
      goalId: goalId || undefined,
      currency: "INR",
    });
  }

  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Calculator className="size-4 text-primary" aria-hidden />
          EMI affordability simulator
        </CardTitle>
        <p className="mt-1 text-sm text-muted-foreground">
          See the monthly cashflow consequence before the purchase.
        </p>
      </CardHeader>
      <CardContent className="grid gap-4">
        <form className="grid gap-3" onSubmit={submit}>
          <div className="grid gap-3 sm:grid-cols-2">
            <LabeledNumber label="Purchase amount" min={1} value={purchaseAmount} onChange={setPurchaseAmount} />
            <LabeledNumber label="Down payment" min={0} value={downPayment} onChange={setDownPayment} />
            <LabeledNumber label="Interest rate" min={0} step={0.1} value={rate} onChange={setRate} suffix="%" />
            <LabeledNumber label="Existing EMIs" min={0} value={existingEmis} onChange={setExistingEmis} />
          </div>
          <label className="grid gap-2 text-sm">
            <span className="font-medium">Tenure: {tenure} months</span>
            <input
              className="accent-primary"
              max="60"
              min="1"
              type="range"
              value={tenure}
              onChange={(event) => setTenure(event.target.value)}
            />
          </label>
          <select
            className="h-10 rounded-lg border border-input bg-background px-3 text-sm outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50"
            value={goalId}
            onChange={(event) => setGoalId(event.target.value)}
          >
            <option value="">Apply goal-delay simulation automatically</option>
            {goals.map((goal) => (
              <option key={goal.id} value={goal.id}>
                {goal.name}
              </option>
            ))}
          </select>
          <Button disabled={loading || Number(purchaseAmount) <= 0}>
            {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <Calculator className="size-4" aria-hidden />}
            Simulate EMI impact
          </Button>
        </form>

        {result ? (
          <div className="grid gap-4">
            <div className="grid gap-3 sm:grid-cols-3">
              <GuidanceMetric label="Monthly EMI" value={formatMoney(result.monthlyEmi)} tone={result.state === "HEALTHY" ? "success" : result.state === "CAUTION" ? "warning" : "info"} />
              <GuidanceMetric label="Free cashflow after" value={formatMoney(result.freeCashflowAfter)} tone={result.freeCashflowAfter >= 0 ? "success" : "warning"} />
              <GuidanceMetric label="Goal delay" value={result.goalDelayMonths ? `${result.goalDelayMonths} mo` : "No delay"} tone="primary" />
            </div>
            <p className="rounded-lg border border-border/70 bg-background/70 p-3 text-sm leading-6 text-muted-foreground">
              {result.explanation}
            </p>
            <CashflowImpactChart data={result.cashflowProjection} />
          </div>
        ) : (
          <EmptyPanel label="Run a scenario to see EMI, savings, goal delay, and cashflow impact." />
        )}
      </CardContent>
    </Card>
  );
}

function ProjectionPanel({
  projection,
  loading,
  onSubmit,
}: {
  projection?: FinancialProjection;
  loading: boolean;
  onSubmit: (payload: { months: number; monthlySavingsOverride?: number; emergencyMonthlyExpenseOverride?: number }) => void;
}) {
  const [months, setMonths] = useState("36");
  const [monthlySavings, setMonthlySavings] = useState("");
  const [monthlyExpense, setMonthlyExpense] = useState("");

  function submit(event: FormEvent) {
    event.preventDefault();
    onSubmit({
      months: Number(months),
      monthlySavingsOverride: monthlySavings ? Number(monthlySavings) : undefined,
      emergencyMonthlyExpenseOverride: monthlyExpense ? Number(monthlyExpense) : undefined,
    });
  }

  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <TrendingUp className="size-4 text-primary" aria-hidden />
          Future projection foundation
        </CardTitle>
        <p className="mt-1 text-sm text-muted-foreground">
          Balance, runway, and long-range target using deterministic monthly savings only.
        </p>
      </CardHeader>
      <CardContent className="grid gap-4">
        <form className="grid gap-3 md:grid-cols-[1fr_1fr_1fr_auto]" onSubmit={submit}>
          <LabeledNumber label="Months" min={1} max={360} value={months} onChange={setMonths} />
          <LabeledNumber label="Monthly savings override" min={0} value={monthlySavings} onChange={setMonthlySavings} />
          <LabeledNumber label="Monthly expense override" min={0} value={monthlyExpense} onChange={setMonthlyExpense} />
          <Button className="self-end" disabled={loading}>
            {loading ? <Loader2 className="size-4 animate-spin" aria-hidden /> : <RefreshCcw className="size-4" aria-hidden />}
            Refresh
          </Button>
        </form>
        {projection ? (
          <>
            <div className="grid gap-3 sm:grid-cols-4">
              <GuidanceMetric label="Current balance" value={formatMoney(projection.currentBalance)} tone="primary" />
              <GuidanceMetric label="Monthly savings" value={formatMoney(projection.monthlySavings)} tone="success" />
              <GuidanceMetric label="Runway" value={`${projection.emergencyRunwayMonths.toFixed(1)} mo`} tone={projection.state === "HEALTHY" ? "success" : "warning"} />
              <GuidanceMetric label="Long-range target" value={formatMoney(projection.fireStyleTarget)} tone="info" />
            </div>
            <ProjectionChart data={projection.trajectory} />
            <div className="grid gap-2">
              {projection.notes.map((note) => (
                <p key={note} className="rounded-lg border border-border/70 bg-background/70 px-3 py-2 text-xs text-muted-foreground">
                  {note}
                </p>
              ))}
            </div>
          </>
        ) : (
          <EmptyPanel label="Projection is preparing from your current balance and cashflow." />
        )}
      </CardContent>
    </Card>
  );
}

function GuidancePrinciples() {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <ShieldCheck className="size-4 text-primary" aria-hidden />
          Confidence rules
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        <PrincipleLine title="No predictions" body="Every value comes from ledger totals, user inputs, and simple deterministic formulas." />
        <PrincipleLine title="No fear language" body="States are healthy, caution, or review so decisions stay calm and clear." />
        <PrincipleLine title="Audit ready" body="Health snapshots, EMI scenarios, and projection runs are persisted for comparison." />
        <PrincipleLine title="Goal consequences" body="EMI impact shows monthly free cashflow and estimated goal-delay months." />
      </CardContent>
    </Card>
  );
}

function PrincipleLine({ title, body }: { title: string; body: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/70 p-3">
      <p className="text-sm font-semibold">{title}</p>
      <p className="mt-1 text-sm leading-6 text-muted-foreground">{body}</p>
    </div>
  );
}

function HealthTrendChart({ data }: { data: FinancialHealthBreakdown["trendHistory"] }) {
  if (data.length === 0) {
    return <EmptyPanel label="Health trend history appears after monthly data builds up." />;
  }
  const points = linePoints(data.map((item) => item.score));
  return (
    <div className="grid gap-3 rounded-lg border border-border/70 bg-background/70 p-3">
      <svg className="h-44 w-full overflow-visible" role="img" aria-label="Financial health trend" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={points.join(" ")} stroke="var(--primary)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
        {points.map((point) => {
          const [x, y] = point.split(",");
          return <circle key={point} cx={x} cy={y} fill="var(--background)" r="1.8" stroke="var(--primary)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />;
        })}
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground sm:grid-cols-6">
        {data.map((item) => <span key={item.periodStart}>{monthLabel(item.periodStart)}</span>)}
      </div>
    </div>
  );
}

function CashflowImpactChart({ data }: { data: CashflowImpactPoint[] }) {
  if (data.length === 0) {
    return <EmptyPanel label="Cashflow impact appears after a valid scenario." />;
  }
  const baseline = data.map((item) => item.baselineFreeCashflow);
  const simulated = data.map((item) => item.simulatedFreeCashflow);
  const allValues = [...baseline, ...simulated];
  const basePoints = linePoints(baseline, allValues);
  const simPoints = linePoints(simulated, allValues);
  return (
    <div className="grid gap-3 rounded-lg border border-border/70 bg-background/70 p-3">
      <svg className="h-44 w-full overflow-visible" role="img" aria-label="EMI cashflow impact" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={basePoints.join(" ")} stroke="var(--success)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" vectorEffect="non-scaling-stroke" />
        <polyline fill="none" points={simPoints.join(" ")} stroke="var(--warning)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
      </svg>
      <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-2"><span className="size-2 rounded-full bg-success" aria-hidden />Before EMI</span>
        <span className="inline-flex items-center gap-2"><span className="size-2 rounded-full bg-warning" aria-hidden />After EMI</span>
      </div>
    </div>
  );
}

function ProjectionChart({ data }: { data: ProjectionPoint[] }) {
  if (data.length === 0) {
    return <EmptyPanel label="Projection points appear after calculation." />;
  }
  const visible = data.filter((_, index) => index % Math.max(1, Math.floor(data.length / 18)) === 0);
  const points = linePoints(visible.map((item) => item.projectedBalance));
  return (
    <div className="grid gap-3 rounded-lg border border-border/70 bg-background/70 p-3">
      <svg className="h-48 w-full overflow-visible" role="img" aria-label="Future balance projection" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={points.join(" ")} stroke="var(--primary)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground sm:grid-cols-6">
        {visible.slice(0, 6).map((item) => <span key={item.monthStart}>{monthLabel(item.monthStart)}</span>)}
      </div>
    </div>
  );
}

function GuidanceMetric({
  label,
  value,
  tone,
}: {
  label: string;
  value: string;
  tone: "primary" | "success" | "warning" | "info";
}) {
  return (
    <div className="rounded-lg border border-border/70 bg-background/70 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn("mt-1 truncate text-sm font-semibold tabular-nums", toneTextClass(tone))}>{value}</p>
    </div>
  );
}

function linePoints(values: number[], domainValues = values) {
  const min = Math.min(...domainValues, 0);
  const max = Math.max(...domainValues, 1);
  const span = Math.max(1, max - min);
  return values.map((value, index) => {
    const x = values.length === 1 ? 50 : (index / (values.length - 1)) * 100;
    const y = 88 - ((value - min) / span) * 72;
    return `${x},${y}`;
  });
}

function LabeledNumber({
  label,
  value,
  onChange,
  min,
  max,
  step,
  suffix,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  min?: number;
  max?: number;
  step?: number;
  suffix?: string;
}) {
  return (
    <label className="grid gap-2 text-sm">
      <span className="font-medium">{label}</span>
      <div className="relative">
        <Input
          className={suffix ? "pr-9" : undefined}
          max={max}
          min={min}
          step={step}
          type="number"
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
        {suffix ? <span className="pointer-events-none absolute right-3 top-2.5 text-sm text-muted-foreground">{suffix}</span> : null}
      </div>
    </label>
  );
}

function InsightCard({ insight, index }: { insight: DeterministicInsight; index: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
      className="rounded-lg border border-border/75 bg-background/70 p-4"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className={cn("size-2.5 rounded-full", stateFillClass(insight.state))} aria-hidden />
            <h3 className="text-sm font-semibold">{insight.title}</h3>
          </div>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">{insight.body}</p>
        </div>
        <span className={cn("rounded-lg px-2 py-1 text-xs font-medium", stateBadgeClass(insight.state))}>
          {stateLabel(insight.state)}
        </span>
      </div>
    </motion.div>
  );
}

function PatternPanel({ title, items }: { title: string; items: RecurringPattern[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Repeat2 className="size-4 text-primary" aria-hidden />
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {items.length === 0 ? (
          <EmptyPanel label="No repeating payments found for this range." />
        ) : (
          items.slice(0, 6).map((item) => (
            <div key={`${item.merchantNormalized}-${item.amount}`} className="grid gap-2 rounded-lg border border-border/70 bg-background/70 p-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{item.merchantName}</p>
                  <p className="text-xs text-muted-foreground">{item.cadence.toLowerCase()} · {item.occurrenceCount} payments · {Math.round(item.confidence)}% confidence</p>
                </div>
                <p className="shrink-0 text-sm font-semibold tabular-nums">{formatMoney(item.amount, item.currency)}</p>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-info transition-[width] duration-500" style={{ width: `${Math.max(8, item.confidence)}%` }} />
              </div>
              <p className="text-xs text-muted-foreground">Next expected {item.nextExpectedOn ?? "after another match"}</p>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function AnomalyPanel({ anomalies }: { anomalies: SpendingAnomaly[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <SearchCheck className="size-4 text-primary" aria-hidden />
          Spending anomaly cards
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {anomalies.length === 0 ? (
          <EmptyPanel label="No category spikes crossed the deterministic threshold." />
        ) : (
          anomalies.map((anomaly) => (
            <div key={anomaly.categoryId ?? anomaly.categoryName} className="rounded-lg border border-border/70 bg-background/70 p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-semibold">{anomaly.categoryName}</p>
                <span className={cn("rounded-lg px-2 py-1 text-xs font-medium", stateBadgeClass(anomaly.state))}>{Math.round(anomaly.changePercent)}%</span>
              </div>
              <p className="mt-2 text-sm text-muted-foreground">{formatMoney(anomaly.currentSpend)} vs {formatMoney(anomaly.baselineSpend)} baseline</p>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}

function CategoryTrendPanel({ trends }: { trends: CategoryTrendInsight[] }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardHeader>
        <CardTitle className="text-base">Category evolution</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {trends.slice(0, 8).map((trend) => (
          <Link
            key={trend.categoryId ?? trend.categoryName}
            href={`/insights/categories/${trend.categoryId ?? "uncategorized"}`}
            className="grid gap-2 rounded-lg border border-border/70 bg-background/70 p-3 transition-colors hover:bg-muted/45"
          >
            <div className="flex items-center justify-between gap-3">
              <p className="truncate text-sm font-semibold">{trend.categoryName}</p>
              <span className={cn("flex items-center gap-1 text-xs font-medium", trend.direction === "UP" ? "text-warning" : "text-success")}>
                {trend.direction === "UP" ? <ArrowUpRight className="size-3" aria-hidden /> : <ArrowDownRight className="size-3" aria-hidden />}
                {Math.round(Math.abs(trend.changePercent))}%
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, Math.max(5, Math.abs(trend.changePercent)))}%` }} />
            </div>
          </Link>
        ))}
      </CardContent>
    </Card>
  );
}

function CategoryDeepDiveGrid({ categories }: { categories: CategoryDeepDive[] }) {
  return (
    <section className="grid gap-4">
      <div>
        <h2 className="text-xl font-semibold">Category deep dives</h2>
        <p className="mt-1 text-sm text-muted-foreground">Category totals, current pace, and recent monthly movement.</p>
      </div>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {categories.slice(0, 9).map((category) => (
          <Link
            key={category.categoryId ?? category.categoryName}
            href={`/insights/categories/${category.categoryId ?? "uncategorized"}`}
            className="rounded-lg border border-border bg-card p-4 shadow-raised transition-colors hover:bg-muted/35"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold">{category.categoryName}</p>
                <p className="mt-1 text-xs text-muted-foreground">Avg {formatMoney(category.averageMonthlySpend)}</p>
              </div>
              <p className="text-sm font-semibold tabular-nums">{formatMoney(category.latestMonthSpend)}</p>
            </div>
            <MiniBars data={category.monthlyValues.map((item) => item.expense)} />
          </Link>
        ))}
      </div>
    </section>
  );
}

function IncomeExpenseChart({ data }: { data: MonthlyComparison[] }) {
  if (data.length === 0) {
    return <EmptyPanel label="No monthly comparison yet." />;
  }
  const max = Math.max(...data.flatMap((item) => [item.income, item.expense]), 1);
  return (
    <div className="grid gap-3">
      {data.map((item) => (
        <div key={item.periodStart} className="grid grid-cols-[3.5rem_1fr] items-center gap-3">
          <span className="text-xs text-muted-foreground">{monthLabel(item.periodStart)}</span>
          <div className="grid gap-1">
            <div className="h-2 rounded-full bg-success/20">
              <div className="h-full rounded-full bg-success transition-[width] duration-500" style={{ width: `${(item.income / max) * 100}%` }} />
            </div>
            <div className="h-2 rounded-full bg-primary/15">
              <div className="h-full rounded-full bg-primary transition-[width] duration-500" style={{ width: `${(item.expense / max) * 100}%` }} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function SavingsChart({ data }: { data: SavingsTrajectory[] }) {
  if (data.length === 0) {
    return <EmptyPanel label="No savings trajectory yet." />;
  }
  const min = Math.min(...data.map((item) => item.cumulativeSavings), 0);
  const max = Math.max(...data.map((item) => item.cumulativeSavings), 1);
  const span = Math.max(1, max - min);
  const points = data.map((item, index) => {
    const x = data.length === 1 ? 50 : (index / (data.length - 1)) * 100;
    const y = 88 - ((item.cumulativeSavings - min) / span) * 72;
    return `${x},${y}`;
  });
  return (
    <div className="grid gap-3">
      <svg className="h-48 w-full overflow-visible" role="img" aria-label="Savings trajectory" viewBox="0 0 100 100" preserveAspectRatio="none">
        <polyline fill="none" points={points.join(" ")} stroke="var(--success)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" vectorEffect="non-scaling-stroke" />
        {points.map((point) => {
          const [x, y] = point.split(",");
          return <circle key={point} cx={x} cy={y} fill="var(--background)" r="1.8" stroke="var(--success)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />;
        })}
      </svg>
      <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground sm:grid-cols-6">
        {data.map((item) => <span key={item.periodStart}>{monthLabel(item.periodStart)}</span>)}
      </div>
    </div>
  );
}

function Timeline({ data }: { data: MonthlyComparison[] }) {
  return (
    <div className="grid gap-3">
      {data.map((item) => (
        <div key={item.periodStart} className="grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-lg border border-border/70 bg-background/70 px-3 py-2">
          <span className="grid size-9 place-items-center rounded-lg bg-primary/10 text-xs font-semibold text-primary">{monthLabel(item.periodStart)}</span>
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">{formatMoney(item.netCashflow)} net</p>
            <p className="text-xs text-muted-foreground">{Math.round(item.savingsRate)}% savings rate</p>
          </div>
          <TrendingUp className={cn("size-4", item.netCashflow >= 0 ? "text-success" : "text-warning")} aria-hidden />
        </div>
      ))}
    </div>
  );
}

function MiniBars({ data }: { data: number[] }) {
  const max = Math.max(...data, 1);
  return (
    <div className="mt-4 flex h-12 items-end gap-1">
      {data.slice(-6).map((value, index) => (
        <span
          key={`${value}-${index}`}
          className="flex-1 rounded-t bg-primary/75"
          style={{ height: `${Math.max(12, (value / max) * 100)}%` }}
          aria-hidden
        />
      ))}
    </div>
  );
}

function MetricTile({ label, value, tone }: { label: string; value: string; tone: "primary" | "success" | "warning" | "info" }) {
  return (
    <Card className="rounded-lg border-border shadow-raised">
      <CardContent className="p-4">
        <p className="text-sm text-muted-foreground">{label}</p>
        <p className={cn("mt-2 truncate text-xl font-semibold tabular-nums", toneTextClass(tone))}>{value}</p>
      </CardContent>
    </Card>
  );
}

function SummaryLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border/70 bg-background/70 px-3 py-2">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

function EmptyPanel({ label }: { label: string }) {
  return (
    <div className="grid min-h-32 place-items-center rounded-lg border border-dashed border-border bg-muted/20 p-5 text-center text-sm text-muted-foreground">
      {label}
    </div>
  );
}

function InsightsLoading() {
  return (
    <main className="grid gap-5">
      <Skeleton className="h-48 w-full" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
      </div>
      <Skeleton className="h-96 w-full" />
    </main>
  );
}

function monthLabel(value: string) {
  return new Date(value).toLocaleDateString("en-IN", { month: "short" });
}

function stateLabel(state: string) {
  if (state === "HEALTHY") {
    return "Healthy";
  }
  if (state === "CAUTION") {
    return "Watch";
  }
  if (state === "RISK") {
    return "Review";
  }
  return "Waiting";
}

function stateFillClass(state: string) {
  if (state === "RISK") {
    return "bg-info";
  }
  if (state === "CAUTION") {
    return "bg-warning";
  }
  return "bg-success";
}

function stateBadgeClass(state: string) {
  if (state === "RISK") {
    return "bg-info/12 text-info";
  }
  if (state === "CAUTION") {
    return "bg-warning/18 text-warning";
  }
  return "bg-success/12 text-success";
}

function toneTextClass(tone: "primary" | "success" | "warning" | "info") {
  if (tone === "success") {
    return "text-success";
  }
  if (tone === "warning") {
    return "text-warning";
  }
  if (tone === "info") {
    return "text-info";
  }
  return "text-primary";
}
