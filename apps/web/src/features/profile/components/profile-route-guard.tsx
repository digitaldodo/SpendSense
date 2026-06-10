"use client";

import { Loader2 } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useSyncExternalStore } from "react";
import { useAuth } from "@/features/auth/hooks/use-auth";
import { getE2eAuthSession } from "@/features/auth/services/auth-client";
import { useProfile } from "@/features/profile/hooks/use-profile";

type ProfileRouteGuardProps = {
  children: React.ReactNode;
  requireOnboardingComplete?: boolean;
  requiredRole?: "ADMIN" | "SUPPORT" | "USER";
};

export function ProfileRouteGuard({
  children,
  requireOnboardingComplete = false,
  requiredRole,
}: ProfileRouteGuardProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { session, isLoading: isAuthLoading } = useAuth();
  const e2eSession = getE2eAuthSession();
  const e2eBypass = useSyncExternalStore(
    subscribeToE2eCookieSnapshot,
    hasLocalE2eCookie,
    () => false,
  );
  const effectiveSession = session ?? e2eSession;
  const effectiveAuthLoading = isAuthLoading && !e2eSession;
  const profileQuery = useProfile(Boolean(effectiveSession));
  const profile = profileQuery.data;

  useEffect(() => {
    if (effectiveAuthLoading) {
      return;
    }

    if (!effectiveSession) {
      router.replace(`/login?redirectTo=${encodeURIComponent(pathname)}`);
      return;
    }

    if (profileQuery.isLoading || !profile) {
      return;
    }

    if (requireOnboardingComplete && !profile.onboardingCompleted) {
      router.replace("/onboarding");
      return;
    }

    if (requiredRole && !profile.roles.includes(requiredRole)) {
      router.replace("/dashboard");
    }
  }, [
    effectiveAuthLoading,
    effectiveSession,
    pathname,
    profile,
    profileQuery.isLoading,
    requireOnboardingComplete,
    requiredRole,
    router,
  ]);

  if (process.env.NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS === "1" || e2eBypass) {
    return <>{children}</>;
  }

  if (effectiveAuthLoading || profileQuery.isLoading || !profile) {
    return (
      <div className="grid min-h-screen place-items-center bg-background">
        <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <Loader2 className="size-4 animate-spin text-primary" aria-hidden />
          Loading your workspace
        </div>
      </div>
    );
  }

  if (requireOnboardingComplete && !profile.onboardingCompleted) {
    return null;
  }

  if (requiredRole && !profile.roles.includes(requiredRole)) {
    return null;
  }

  return <>{children}</>;
}

function hasLocalE2eCookie() {
  return (
    typeof window !== "undefined" &&
    ["localhost", "127.0.0.1"].includes(window.location.hostname) &&
    document.cookie.includes("__spendsense_e2e_session=1")
  );
}

function subscribeToE2eCookieSnapshot() {
  return () => undefined;
}
