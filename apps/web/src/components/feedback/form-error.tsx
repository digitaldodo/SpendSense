import { AlertCircle } from "lucide-react";

type FormErrorProps = {
  message?: string;
};

export function FormError({ message }: FormErrorProps) {
  if (!message) {
    return null;
  }

  return (
    <p className="flex items-center gap-2 text-sm text-destructive">
      <AlertCircle className="size-4" aria-hidden />
      {message}
    </p>
  );
}
