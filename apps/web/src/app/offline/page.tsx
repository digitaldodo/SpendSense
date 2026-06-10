import { WifiOff } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export const metadata = {
  title: "Offline",
};

export default function OfflinePage() {
  return (
    <main className="container-app grid min-h-screen place-items-center py-section-y">
      <section className="grid max-w-md gap-4 rounded-lg border border-border bg-card p-6 text-center shadow-raised">
        <WifiOff className="mx-auto size-8 text-primary" aria-hidden />
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold">You are offline.</h1>
          <p className="text-sm leading-6 text-muted-foreground">
            SpendSense can keep the app shell available while your network returns. Live balances
            and imports resume once the API is reachable.
          </p>
        </div>
        <Button className="mx-auto" variant="outline" render={<Link href="/dashboard" />}>
          Return to dashboard
        </Button>
      </section>
    </main>
  );
}
