import { ArrowRight, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function Home() {
  return (
    <main className="container-app flex min-h-screen items-center py-section-y">
      <section className="max-w-2xl space-y-7">
        <div className="inline-flex items-center gap-2 rounded-md border border-border bg-card px-3 py-1 text-sm text-muted-foreground">
          <ShieldCheck className="size-4 text-primary" aria-hidden />
          Supabase-secured access
        </div>
        <div className="space-y-4">
          <h1 className="text-4xl font-semibold leading-tight text-foreground sm:text-5xl">
            SpendSense authentication is ready for protected product work.
          </h1>
          <p className="max-w-xl text-base leading-7 text-muted-foreground sm:text-lg">
            Sign in or create an account to enter the protected app shell. Financial workflows
            remain intentionally out of scope for this phase.
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
    </main>
  );
}
