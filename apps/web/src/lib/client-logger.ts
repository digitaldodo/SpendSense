"use client";

import { env, isProductionApp } from "@/config/env";

type ClientLogLevel = "info" | "warn" | "error";

type ClientLogPayload = {
  component?: string;
  context?: Record<string, unknown>;
  error?: unknown;
  level: ClientLogLevel;
  message: string;
};

export function logClientEvent(payload: ClientLogPayload) {
  const normalized = {
    app: "spendsense-web",
    component: payload.component ?? "runtime",
    context: payload.context ?? {},
    level: payload.level,
    message: payload.message,
    release: env.NEXT_PUBLIC_APP_VERSION,
  };

  if (!isProductionApp || payload.level === "error") {
    const method = payload.level === "error" ? "error" : payload.level;
    console[method](normalized.message, normalized, payload.error ?? "");
  }

  if (payload.level === "error") {
    captureSentryReadyError(payload.error, normalized);
  }
}

function captureSentryReadyError(error: unknown, context: Record<string, unknown>) {
  const sentry = typeof window !== "undefined" ? window.Sentry : undefined;
  if (!sentry?.captureException) {
    return;
  }
  sentry.captureException(error instanceof Error ? error : new Error(String(error)), {
    extra: context,
    tags: {
      app: "spendsense-web",
      release: env.NEXT_PUBLIC_APP_VERSION,
    },
  });
}

declare global {
  interface Window {
    Sentry?: {
      captureException?: (
        error: Error,
        context?: { extra?: Record<string, unknown>; tags?: Record<string, string> }
      ) => void;
    };
  }
}
