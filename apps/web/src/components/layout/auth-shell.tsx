export function AuthShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="grid min-h-screen place-items-center bg-background px-page-x py-section-y">
      <div className="w-full max-w-md">{children}</div>
    </main>
  );
}
