import type { ComponentType } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function FinancialCard({
  title,
  value,
  detail,
  icon: Icon,
  className,
}: {
  title: string;
  value: string;
  detail?: string;
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  className?: string;
}) {
  return (
    <Card className={cn("rounded-lg border-border/80 shadow-raised", className)}>
      <CardHeader className="grid grid-cols-[1fr_auto] items-start gap-3">
        <div className="space-y-1">
          <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
          <p className="text-2xl font-semibold tracking-normal text-foreground">{value}</p>
        </div>
        <span className="grid size-9 place-items-center rounded-lg bg-primary/10 text-primary">
          <Icon className="size-4" aria-hidden />
        </span>
      </CardHeader>
      {detail ? (
        <CardContent>
          <p className="text-sm leading-5 text-muted-foreground">{detail}</p>
        </CardContent>
      ) : null}
    </Card>
  );
}
