"use client";

import { AlertTriangle } from "lucide-react";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { logClientEvent } from "@/lib/client-logger";
import "./globals.css";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    logClientEvent({
      component: "global-error-boundary",
      context: { digest: error.digest },
      error,
      level: "error",
      message: "Global app render failed",
    });
  }, [error]);

  return (
    <html lang="en">
      <body>
        <main className="container-app grid min-h-screen place-items-center py-section-y">
          <section className="grid max-w-md gap-4 rounded-lg border border-border bg-card p-6 text-center shadow-raised">
            <AlertTriangle className="mx-auto size-8 text-primary" aria-hidden />
            <div className="space-y-2">
              <h1 className="text-2xl font-semibold">SpendSense needs a quick refresh.</h1>
              <p className="text-sm leading-6 text-muted-foreground">
                The app shell recovered from a runtime fault and kept sensitive state out of the
                error screen.
              </p>
            </div>
            <Button className="mx-auto" onClick={reset}>
              Reload workspace
            </Button>
          </section>
        </main>
      </body>
    </html>
  );
}
