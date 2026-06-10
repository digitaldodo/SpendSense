import { Compass } from "lucide-react";
import Link from "next/link";
import { SpendSenseLogo } from "@/components/brand/spendsense-logo";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <main className="container-app grid min-h-screen place-items-center py-section-y">
      <section className="grid max-w-md gap-4 rounded-lg border border-border bg-card p-6 text-center shadow-raised">
        <div className="mx-auto grid justify-items-center gap-2">
          <SpendSenseLogo variant="mark" size="lg" priority />
          <Compass className="size-5 text-muted-foreground" aria-hidden />
        </div>
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold">This SpendSense view is not available.</h1>
          <p className="text-sm leading-6 text-muted-foreground">
            The route may have moved, or your workspace may not include it yet.
          </p>
        </div>
        <Button className="mx-auto" render={<Link href="/dashboard" />}>
          Open dashboard
        </Button>
      </section>
    </main>
  );
}
