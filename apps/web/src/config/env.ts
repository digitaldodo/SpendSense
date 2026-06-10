import { z } from "zod";

const envSchema = z.object({
  NEXT_PUBLIC_APP_ENV: z
    .enum(["local", "development", "staging", "production"])
    .default("local"),
  NEXT_PUBLIC_SITE_URL: z.string().url().default("http://localhost:3000"),
  NEXT_PUBLIC_API_BASE_URL: z.string().url().default("http://localhost:8080"),
  NEXT_PUBLIC_SUPABASE_URL: z.string().url().default("http://localhost:54321"),
  NEXT_PUBLIC_SUPABASE_ANON_KEY: z.string().min(1).default("local-development-anon-key"),
  NEXT_PUBLIC_SENTRY_DSN: z.string().url().optional(),
  NEXT_PUBLIC_APP_VERSION: z.string().default("0.1.0"),
  NEXT_PUBLIC_RELEASE_COMMIT: z.string().default("local"),
  NEXT_PUBLIC_MAINTENANCE_MODE: z.enum(["0", "1"]).default("0"),
  NEXT_PUBLIC_DEGRADED_MODE: z.enum(["0", "1"]).default("0"),
  NEXT_PUBLIC_FEATURE_FLAGS: z.string().default("{}"),
  NEXT_PUBLIC_ANALYTICS_WRITE_KEY: z.string().optional(),
  NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS: z.enum(["0", "1"]).default("0"),
}).superRefine((values, context) => {
  const managedEnv =
    values.NEXT_PUBLIC_APP_ENV === "staging" || values.NEXT_PUBLIC_APP_ENV === "production";
  if (!managedEnv) {
    return;
  }

  const urlValues = [
    ["NEXT_PUBLIC_SITE_URL", values.NEXT_PUBLIC_SITE_URL],
    ["NEXT_PUBLIC_API_BASE_URL", values.NEXT_PUBLIC_API_BASE_URL],
    ["NEXT_PUBLIC_SUPABASE_URL", values.NEXT_PUBLIC_SUPABASE_URL],
  ] as const;

  for (const [key, value] of urlValues) {
    if (!value.startsWith("https://") || value.includes("localhost") || value.includes("127.0.0.1")) {
      context.addIssue({
        code: "custom",
        message: `${key} must be an HTTPS managed URL for staging and production builds.`,
        path: [key],
      });
    }
  }

  if (
    values.NEXT_PUBLIC_SUPABASE_ANON_KEY.includes("your-") ||
    values.NEXT_PUBLIC_SUPABASE_ANON_KEY === "ci-anon-key"
  ) {
    context.addIssue({
      code: "custom",
      message: "NEXT_PUBLIC_SUPABASE_ANON_KEY must come from managed environment secrets.",
      path: ["NEXT_PUBLIC_SUPABASE_ANON_KEY"],
    });
  }
});

export const env = envSchema.parse({
  NEXT_PUBLIC_APP_ENV: process.env.NEXT_PUBLIC_APP_ENV,
  NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL,
  NEXT_PUBLIC_SUPABASE_ANON_KEY: process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY,
  NEXT_PUBLIC_SENTRY_DSN: process.env.NEXT_PUBLIC_SENTRY_DSN || undefined,
  NEXT_PUBLIC_APP_VERSION: process.env.NEXT_PUBLIC_APP_VERSION,
  NEXT_PUBLIC_RELEASE_COMMIT: process.env.NEXT_PUBLIC_RELEASE_COMMIT ?? process.env.VERCEL_GIT_COMMIT_SHA,
  NEXT_PUBLIC_MAINTENANCE_MODE: process.env.NEXT_PUBLIC_MAINTENANCE_MODE,
  NEXT_PUBLIC_DEGRADED_MODE: process.env.NEXT_PUBLIC_DEGRADED_MODE,
  NEXT_PUBLIC_FEATURE_FLAGS: process.env.NEXT_PUBLIC_FEATURE_FLAGS,
  NEXT_PUBLIC_ANALYTICS_WRITE_KEY: process.env.NEXT_PUBLIC_ANALYTICS_WRITE_KEY || undefined,
  NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS: process.env.NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS,
});

export const isProductionApp = env.NEXT_PUBLIC_APP_ENV === "production";
export const isManagedApp = env.NEXT_PUBLIC_APP_ENV === "staging" || isProductionApp;
export const isMaintenanceMode = env.NEXT_PUBLIC_MAINTENANCE_MODE === "1";
export const isDegradedMode = env.NEXT_PUBLIC_DEGRADED_MODE === "1";

export function getFeatureFlags(): Record<string, boolean> {
  try {
    const parsed = JSON.parse(env.NEXT_PUBLIC_FEATURE_FLAGS);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    return Object.fromEntries(
      Object.entries(parsed).filter((entry): entry is [string, boolean] => typeof entry[1] === "boolean")
    );
  } catch {
    return {};
  }
}
