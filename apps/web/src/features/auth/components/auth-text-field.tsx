import type { FieldError, UseFormRegisterReturn } from "react-hook-form";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type AuthTextFieldProps = {
  label: string;
  type?: string;
  placeholder: string;
  autoComplete: string;
  registration: UseFormRegisterReturn;
  error?: FieldError;
};

export function AuthTextField({
  label,
  type = "text",
  placeholder,
  autoComplete,
  registration,
  error,
}: AuthTextFieldProps) {
  return (
    <label className="grid gap-2 text-sm font-medium text-foreground">
      <span>{label}</span>
      <Input
        type={type}
        placeholder={placeholder}
        autoComplete={autoComplete}
        aria-invalid={error ? true : undefined}
        className={cn("h-10 bg-background/70", error && "border-destructive")}
        {...registration}
      />
      {error ? <span className="text-xs font-normal text-destructive">{error.message}</span> : null}
    </label>
  );
}
