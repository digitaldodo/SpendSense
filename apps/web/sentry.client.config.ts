import * as Sentry from "@sentry/nextjs";

const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN;

if (dsn) {
  Sentry.init({
    dsn,
    environment: process.env.NEXT_PUBLIC_APP_ENV ?? "local",
    release: process.env.NEXT_PUBLIC_APP_VERSION,
    tracesSampleRate: Number(process.env.NEXT_PUBLIC_SENTRY_TRACES_SAMPLE_RATE ?? "0.05"),
    replaysOnErrorSampleRate: 0.1,
    replaysSessionSampleRate: 0,
    sendDefaultPii: false,
  });
}
