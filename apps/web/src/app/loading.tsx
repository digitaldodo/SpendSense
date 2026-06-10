import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <main className="container-app grid min-h-screen content-start gap-5 py-section-y">
      <Skeleton className="h-24 w-full" />
      <div className="grid gap-4 md:grid-cols-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
      <Skeleton className="h-80 w-full" />
    </main>
  );
}
