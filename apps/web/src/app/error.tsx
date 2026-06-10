"use client";

import { AlertTriangle, RotateCw } from "lucide-react";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { logClientEvent } from "@/lib/client-logger";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    logClientEvent({
      component: "app-error-boundary",
      context: { digest: error.digest },
      error,
      level: "error",
      message: "App route render failed",
    });
  }, [error]);

  return (
    <main className="container-app grid min-h-screen place-items-center py-section-y">
      <section className="grid max-w-md gap-4 rounded-lg border border-border bg-card p-6 text-center shadow-raised">
        <AlertTriangle className="mx-auto size-8 text-primary" aria-hidden />
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold">SpendSense hit a temporary issue.</h1>
          <p className="text-sm leading-6 text-muted-foreground">
            Your session is still protected. Retry the view or return to the dashboard once the
            connection settles.
          </p>
        </div>
        <Button className="mx-auto" onClick={reset}>
          <RotateCw className="size-4" aria-hidden />
          Retry
        </Button>
      </section>
    </main>
  );
}
