import { ArrowRight, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { SpendSenseLogo } from "@/components/brand/spendsense-logo";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function Home() {
  return (
    <main className="min-h-screen bg-[linear-gradient(145deg,var(--background)_0%,color-mix(in_oklch,var(--primary),white_92%)_46%,color-mix(in_oklch,var(--accent),white_86%)_100%)]">
      <div className="container-app flex min-h-screen items-center py-section-y">
        <section className="max-w-2xl space-y-7">
          <SpendSenseLogo size="xl" subtitle="Calm financial intelligence" priority />
          <div className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-1 text-sm text-muted-foreground">
            <ShieldCheck className="size-4 text-primary" aria-hidden />
            Supabase-secured access
          </div>
          <div className="space-y-4">
            <h1 className="text-4xl font-semibold leading-tight text-foreground sm:text-5xl">
              SpendSense
            </h1>
            <p className="max-w-xl text-base leading-7 text-muted-foreground sm:text-lg">
              Sign in or create an account to enter a protected finance workspace for imports,
              spending visibility, budgets, goals, and calm money decisions.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Link className={cn(buttonVariants({ className: "h-10 gap-2" }))} href="/login">
              Sign in
              <ArrowRight className="size-4" aria-hidden />
            </Link>
            <Link
              className={cn(buttonVariants({ variant: "outline", className: "h-10" }))}
              href="/signup"
            >
              Create account
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}
