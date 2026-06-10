import type { captureRouterTransitionStart } from "@sentry/nextjs";

if (process.env.NEXT_PUBLIC_SENTRY_DSN) {
  void import("../sentry.client.config");
}

export function onRouterTransitionStart(...args: Parameters<typeof captureRouterTransitionStart>) {
  if (!process.env.NEXT_PUBLIC_SENTRY_DSN) {
    return;
  }
  void import("@sentry/nextjs").then((Sentry) => {
    Sentry.captureRouterTransitionStart(...args);
  });
}
