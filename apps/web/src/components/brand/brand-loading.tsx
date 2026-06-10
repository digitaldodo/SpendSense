import { SpendSenseLogo } from "@/components/brand/spendsense-logo";
import { cn } from "@/lib/utils";

type BrandLoadingProps = {
  className?: string;
  label?: string;
  size?: "sm" | "md" | "lg";
};

const sizeClasses = {
  sm: "size-12",
  md: "size-16",
  lg: "size-20",
};

export function BrandLoading({
  className,
  label = "Loading SpendSense",
  size = "md",
}: BrandLoadingProps) {
  return (
    <div className={cn("grid justify-items-center gap-4 text-center", className)} role="status">
      <div
        className={cn(
          "grid place-items-center rounded-full bg-card/75 ring-1 ring-border/70 backdrop-blur",
          sizeClasses[size]
        )}
      >
        <SpendSenseLogo
          variant="mark"
          size={size === "lg" ? "lg" : size === "md" ? "md" : "sm"}
          imageClassName="animate-[spendsense-logo-breathe_1.8s_ease-in-out_infinite]"
          priority
        />
      </div>
      <p className="text-sm font-medium text-muted-foreground">{label}</p>
    </div>
  );
}
