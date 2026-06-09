import { ArrowRight, ShieldCheck, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const foundations = [
  "Design tokens",
  "App shell",
  "Data provider",
  "Validation utilities",
  "API client",
  "Route groups",
];

export default function Home() {
  return (
    <main className="section-spacing container-app">
      <div className="grid gap-10 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
        <section className="space-y-8">
          <Badge variant="secondary" className="w-fit">
            Phase 1 foundation
          </Badge>
          <div className="max-w-3xl space-y-5">
            <h1 className="text-4xl font-semibold leading-tight text-foreground sm:text-5xl">
              SpendSense is ready for product systems to land on solid ground.
            </h1>
            <p className="max-w-2xl text-lg leading-8 text-muted-foreground">
              This shell contains the design, state, validation, API, and layout primitives for
              future authentication, financial workflows, AI guidance, and analytics.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button className="gap-2">
              Continue scaffold review
              <ArrowRight className="size-4" aria-hidden />
            </Button>
            <Button variant="outline" className="gap-2">
              <ShieldCheck className="size-4" aria-hidden />
              Infrastructure only
            </Button>
          </div>
        </section>

        <Card className="surface-raised border-border/80">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-xl">
              <Sparkles className="size-5 text-primary" aria-hidden />
              Prepared layers
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
            {foundations.map((item) => (
              <div
                key={item}
                className="rounded-md border border-border bg-secondary/50 px-4 py-3 text-sm font-medium"
              >
                {item}
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
