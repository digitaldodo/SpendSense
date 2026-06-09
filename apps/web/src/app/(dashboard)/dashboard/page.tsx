import { ArrowRight, CheckCircle2, CircleDashed, FileText, ShieldCheck, Sparkles } from "lucide-react";
import type { ComponentType } from "react";

export const metadata = {
  title: "Dashboard",
};

export default function DashboardPage() {
  return (
    <main className="grid gap-5">
      <section className="grid gap-4 rounded-lg border border-border/70 bg-card/86 p-5 shadow-raised backdrop-blur sm:p-6 lg:grid-cols-[1.3fr_0.7fr]">
        <div className="space-y-4">
          <div className="inline-flex items-center gap-2 rounded-lg border border-border bg-muted/55 px-3 py-1.5 text-sm font-medium text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden />
            Onboarding complete
          </div>
          <div className="space-y-2">
            <h2 className="text-2xl font-semibold leading-tight sm:text-3xl">
              Your SpendSense workspace is ready.
            </h2>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground sm:text-base">
              This shell is intentionally quiet: profile, navigation, account context, and empty
              states are ready without inventing transactions, charts, scores, or advice.
            </p>
          </div>
        </div>
        <div className="grid content-end gap-3 rounded-lg border border-border bg-muted/35 p-4">
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-lg bg-success/12 text-success">
              <CheckCircle2 className="size-5" aria-hidden />
            </span>
            <div>
              <p className="text-sm font-semibold">Profile foundation</p>
              <p className="text-xs text-muted-foreground">Persisted and protected</p>
            </div>
          </div>
          <div className="h-px bg-border" />
          <div className="flex items-center gap-3">
            <span className="grid size-9 place-items-center rounded-lg bg-primary/10 text-primary">
              <CircleDashed className="size-5" aria-hidden />
            </span>
            <div>
              <p className="text-sm font-semibold">Financial data</p>
              <p className="text-xs text-muted-foreground">Reserved for the next phase</p>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <EmptyState
          icon={FileText}
          title="No transactions yet"
          description="Ingestion is not part of this phase, so the workspace stays clean until real data exists."
        />
        <EmptyState
          icon={Sparkles}
          title="No insights yet"
          description="The analytics and mentor layers will appear only after their backend foundations exist."
        />
        <EmptyState
          icon={ArrowRight}
          title="Next best action"
          description="Continue with data connections and transaction models in Phase 4."
        />
      </section>
    </main>
  );
}

function EmptyState({
  icon: Icon,
  title,
  description,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  title: string;
  description: string;
}) {
  return (
    <article className="grid min-h-44 content-start gap-3 rounded-lg border border-border/70 bg-card/82 p-5 shadow-raised">
      <span className="grid size-10 place-items-center rounded-lg bg-muted text-primary">
        <Icon className="size-5" aria-hidden />
      </span>
      <div className="space-y-1">
        <h3 className="text-base font-semibold">{title}</h3>
        <p className="text-sm leading-6 text-muted-foreground">{description}</p>
      </div>
    </article>
  );
}
