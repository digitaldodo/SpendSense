import Image from "next/image";
import { cn } from "@/lib/utils";

type SpendSenseLogoSize = "xs" | "sm" | "md" | "lg" | "xl";

type SpendSenseLogoProps = {
  className?: string;
  imageClassName?: string;
  priority?: boolean;
  size?: SpendSenseLogoSize;
  subtitle?: string;
  title?: string;
  variant?: "mark" | "lockup";
};

const markSizeClasses: Record<SpendSenseLogoSize, string> = {
  xs: "size-7",
  sm: "size-9",
  md: "size-11",
  lg: "size-14",
  xl: "size-20",
};

const markImageSizes: Record<SpendSenseLogoSize, string> = {
  xs: "28px",
  sm: "36px",
  md: "44px",
  lg: "56px",
  xl: "80px",
};

const titleSizeClasses: Record<SpendSenseLogoSize, string> = {
  xs: "text-sm",
  sm: "text-sm",
  md: "text-base",
  lg: "text-lg",
  xl: "text-xl",
};

export function SpendSenseLogo({
  className,
  imageClassName,
  priority = false,
  size = "md",
  subtitle,
  title = "SpendSense",
  variant = "lockup",
}: SpendSenseLogoProps) {
  const mark = (
    <span
      className={cn(
        "relative grid shrink-0 place-items-center overflow-visible rounded-md transition-transform duration-200 ease-out group-hover/logo:scale-[1.03]",
        markSizeClasses[size],
        imageClassName
      )}
      aria-hidden={variant === "lockup"}
    >
      <Image
        src="/brand/spendsense-mark-512.png"
        alt={variant === "mark" ? title : ""}
        fill
        priority={priority}
        sizes={markImageSizes[size]}
        className="object-contain drop-shadow-[0_1px_1px_rgb(0_0_0_/_0.10)]"
      />
    </span>
  );

  if (variant === "mark") {
    return (
      <span className={cn("inline-flex items-center", className)} aria-label={title}>
        {mark}
      </span>
    );
  }

  return (
    <span className={cn("inline-flex min-w-0 items-center gap-3", className)}>
      {mark}
      <span className="min-w-0 leading-none">
        <span
          className={cn("block truncate font-semibold text-foreground", titleSizeClasses[size])}
        >
          {title}
        </span>
        {subtitle ? (
          <span className="mt-1 block truncate text-xs leading-4 text-muted-foreground">
            {subtitle}
          </span>
        ) : null}
      </span>
    </span>
  );
}
