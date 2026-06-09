export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <div className="container-dashboard py-6">{children}</div>
    </div>
  );
}
