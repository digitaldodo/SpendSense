import { SpendSenseLogo } from "@/components/brand/spendsense-logo";

export function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="grid min-h-screen place-items-center bg-[linear-gradient(150deg,var(--background)_0%,color-mix(in_oklch,var(--primary),white_92%)_48%,color-mix(in_oklch,var(--accent),white_88%)_100%)] px-page-x py-section-y">
      <div className="w-full max-w-md space-y-6">
        <div className="flex justify-center">
          <SpendSenseLogo
            className="justify-center"
            size="lg"
            subtitle="Calm financial intelligence"
            priority
          />
        </div>
        {children}
      </div>
    </main>
  );
}
