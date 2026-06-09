import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

type AuthFormCardProps = {
  eyebrow: string;
  title: string;
  description: string;
  children: React.ReactNode;
  footerLabel: string;
  footerHref: string;
  footerAction: string;
};

export function AuthFormCard({
  eyebrow,
  title,
  description,
  children,
  footerLabel,
  footerHref,
  footerAction,
}: AuthFormCardProps) {
  return (
    <Card className="surface-raised rounded-lg border-border/80 bg-card/95">
      <CardHeader className="space-y-3 px-6 pt-7 pb-4">
        <p className="text-xs font-medium tracking-[0.12em] text-muted-foreground uppercase">
          {eyebrow}
        </p>
        <div className="space-y-2">
          <CardTitle className="text-2xl font-semibold">{title}</CardTitle>
          <p className="text-sm leading-6 text-muted-foreground">{description}</p>
        </div>
      </CardHeader>
      <CardContent className="space-y-5 px-6 pb-7">
        {children}
        <p className="text-center text-sm text-muted-foreground">
          {footerLabel}{" "}
          <Link className="font-medium text-primary transition-colors hover:text-primary/80" href={footerHref}>
            {footerAction}
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
