import { AlertTriangle, BadgeInfo, Wrench } from "lucide-react";
import { env, isDegradedMode, isMaintenanceMode } from "@/config/env";
import { cn } from "@/lib/utils";

export function OperationalBanner() {
  const appEnv = env.NEXT_PUBLIC_APP_ENV;
  const showEnvironment = appEnv !== "production";
  const visible = showEnvironment || isMaintenanceMode || isDegradedMode;

  if (!visible) {
    return null;
  }

  const Icon = isMaintenanceMode ? Wrench : isDegradedMode ? AlertTriangle : BadgeInfo;
  const tone = isMaintenanceMode
    ? "border-warning/35 bg-warning/10 text-foreground"
    : isDegradedMode
      ? "border-warning/30 bg-warning/10 text-foreground"
    : "border-primary/20 bg-primary/10 text-foreground";
  const label = isMaintenanceMode
    ? "Maintenance mode"
    : isDegradedMode
      ? "Some services are slower than usual"
      : appEnv === "staging"
        ? "Staging rehearsal"
        : `${appEnv} environment`;

  return (
    <div className={cn("border-b px-4 py-2 text-sm", tone)}>
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2">
        <div className="flex min-w-0 items-center gap-2">
          <Icon className="size-4 shrink-0 text-primary" aria-hidden />
          <span className="font-medium">{label}</span>
          <span className="text-muted-foreground">
            {isMaintenanceMode
              ? "SpendSense is in a protected read-only window while operational work completes."
              : isDegradedMode
                ? "Your data remains protected; delivery, reports, or live updates may take longer."
                : appEnv === "staging"
                  ? "Production-like checks only; do not use production credentials or customer data here."
                  : "Use this workspace for validation only."}
          </span>
        </div>
        <span className="font-mono text-xs text-muted-foreground" title={env.NEXT_PUBLIC_RELEASE_COMMIT}>
          {env.NEXT_PUBLIC_APP_VERSION} · {shortCommit(env.NEXT_PUBLIC_RELEASE_COMMIT)}
        </span>
      </div>
    </div>
  );
}

function shortCommit(value: string) {
  return value.length > 8 ? value.slice(0, 8) : value;
}
