"use client";

import { DashboardShell } from "@/components/layout/dashboard-shell";
import { ProfileRouteGuard } from "@/features/profile/components/profile-route-guard";

export function AdminShell({ children }: { children: React.ReactNode }) {
  return (
    <ProfileRouteGuard requireOnboardingComplete requiredRole="ADMIN">
      <DashboardShell>{children}</DashboardShell>
    </ProfileRouteGuard>
  );
}
