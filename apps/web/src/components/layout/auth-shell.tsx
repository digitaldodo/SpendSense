export function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="grid min-h-screen place-items-center bg-background px-page-x py-section-y">
      <div className="w-full max-w-md space-y-6">
        <div className="space-y-1 text-center">
          <p className="text-xl font-semibold tracking-tight">SpendSense</p>
          <p className="text-sm text-muted-foreground">Calm financial intelligence</p>
        </div>
        {children}
      </div>
    </main>
  );
}
