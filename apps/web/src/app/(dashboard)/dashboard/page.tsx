import { ShieldCheck } from "lucide-react";

export const metadata = {
  title: "Dashboard",
};

export default function DashboardPage() {
  return (
    <main className="grid gap-4">
      <section className="rounded-lg border border-border bg-card p-5 surface-raised">
        <div className="flex items-start gap-3">
          <div className="rounded-md bg-primary/10 p-2 text-primary">
            <ShieldCheck className="size-5" aria-hidden />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">Authentication active</h2>
            <p className="max-w-2xl text-sm leading-6 text-muted-foreground">
              This protected shell confirms session routing, refresh, and logout behavior. Product
              dashboards and financial workflows are intentionally reserved for the next phase.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
