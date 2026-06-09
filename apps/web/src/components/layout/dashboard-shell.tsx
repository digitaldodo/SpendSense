import { LogoutButton } from "@/features/auth/components/logout-button";

export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card/70">
        <div className="container-dashboard flex min-h-16 items-center justify-between gap-4 py-3">
          <div>
            <p className="text-sm font-medium text-muted-foreground">SpendSense</p>
            <h1 className="text-lg font-semibold">Protected workspace</h1>
          </div>
          <LogoutButton />
        </div>
      </header>
      <div className="container-dashboard py-6">{children}</div>
    </div>
  );
}
