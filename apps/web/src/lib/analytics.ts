"use client";

import { env, isProductionApp } from "@/config/env";
import { logClientEvent } from "@/lib/client-logger";

type AnalyticsEvent = {
  name: string;
  properties?: Record<string, string | number | boolean | null>;
};

export function trackAnalyticsEvent(event: AnalyticsEvent) {
  if (!env.NEXT_PUBLIC_ANALYTICS_WRITE_KEY) {
    if (!isProductionApp) {
      logClientEvent({
        component: "analytics",
        context: { event: event.name, properties: event.properties ?? {} },
        level: "info",
        message: "Analytics event queued for future provider",
      });
    }
    return;
  }

  logClientEvent({
    component: "analytics",
    context: { event: event.name },
    level: "info",
    message: "Analytics provider hook is configured",
  });
}
