"use client";

import { Loader2 } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/features/auth/hooks/use-auth";
import { useProfile } from "@/features/profile/hooks/use-profile";

type ProfileRouteGuardProps = {
  children: React.ReactNode;
  requireOnboardingComplete?: boolean;
};

export function ProfileRouteGuard({
  children,
  requireOnboardingComplete = false,
}: ProfileRouteGuardProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { session, isLoading: isAuthLoading } = useAuth();
  const profileQuery = useProfile(Boolean(session));
  const profile = profileQuery.data;

  useEffect(() => {
    if (isAuthLoading) {
      return;
    }

    if (!session) {
      router.replace(`/login?redirectTo=${encodeURIComponent(pathname)}`);
      return;
    }

    if (profileQuery.isLoading || !profile) {
      return;
    }

    if (requireOnboardingComplete && !profile.onboardingCompleted) {
      router.replace("/onboarding");
    }
  }, [
    isAuthLoading,
    pathname,
    profile,
    profileQuery.isLoading,
    requireOnboardingComplete,
    router,
    session,
  ]);

  if (isAuthLoading || profileQuery.isLoading || !profile) {
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

  return <>{children}</>;
}
