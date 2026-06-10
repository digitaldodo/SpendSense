import { getFeatureFlags } from "@/config/env";

const flags = getFeatureFlags();

export function isFeatureEnabled(flag: string) {
  return Boolean(flags[flag]);
}

export function featureFlagSnapshot() {
  return { ...flags };
}
