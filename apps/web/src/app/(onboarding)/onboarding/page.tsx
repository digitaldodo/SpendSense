import { ProfileRouteGuard } from "@/features/profile/components/profile-route-guard";
import { OnboardingFlow } from "@/features/onboarding/components/onboarding-flow";

export const metadata = {
  title: "Onboarding",
};

export default function OnboardingPage() {
  return (
    <ProfileRouteGuard>
      <OnboardingFlow />
    </ProfileRouteGuard>
  );
}
