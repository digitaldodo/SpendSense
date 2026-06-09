import { zodResolver } from "@hookform/resolvers/zod";
import type { FieldValues, Resolver, UseFormProps } from "react-hook-form";
import type { z } from "zod";

export function createZodFormOptions<TValues extends FieldValues>(
  schema: z.ZodType<TValues, FieldValues>,
  options?: Omit<UseFormProps<TValues>, "resolver">
): UseFormProps<TValues> {
  return {
    mode: "onBlur",
    reValidateMode: "onChange",
    ...options,
    resolver: zodResolver(schema) as Resolver<TValues>,
  };
}
