import { BrandLoading } from "@/components/brand/brand-loading";
import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <main className="container-app grid min-h-screen content-start gap-5 py-section-y">
      <div className="grid min-h-40 place-items-center rounded-lg border border-border/70 bg-card/70 p-6 backdrop-blur">
        <BrandLoading label="Preparing your SpendSense workspace" />
      </div>
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
