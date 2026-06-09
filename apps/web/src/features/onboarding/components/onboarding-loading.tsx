import { Skeleton } from "@/components/ui/skeleton";

export function OnboardingLoading() {
  return (
    <main className="min-h-screen bg-background px-4 py-6">
      <div className="mx-auto grid min-h-[calc(100vh-3rem)] w-full max-w-5xl content-center gap-6">
        <Skeleton className="h-2 w-full rounded-full" />
        <div className="grid gap-3">
          <Skeleton className="h-10 w-3/4" />
          <Skeleton className="h-5 w-full max-w-xl" />
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
      </div>
    </main>
  );
}
