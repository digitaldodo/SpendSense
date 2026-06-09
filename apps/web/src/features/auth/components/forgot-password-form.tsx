"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormError } from "@/components/feedback/form-error";
import { AuthFormCard } from "@/features/auth/components/auth-form-card";
import { AuthTextField } from "@/features/auth/components/auth-text-field";
import {
  forgotPasswordSchema,
  type ForgotPasswordFormValues,
} from "@/features/auth/schemas/auth-schemas";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";

export function ForgotPasswordForm() {
  const [formError, setFormError] = useState<string>();
  const [successMessage, setSuccessMessage] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const form = useForm<ForgotPasswordFormValues>({
    mode: "onBlur",
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  async function onSubmit(values: ForgotPasswordFormValues) {
    setIsSubmitting(true);
    setFormError(undefined);
    setSuccessMessage(undefined);

    const supabase = getSupabaseBrowserClient();
    const { error } = await supabase.auth.resetPasswordForEmail(values.email, {
      redirectTo: `${window.location.origin}/login`,
    });

    if (error) {
      setFormError(error.message);
      setIsSubmitting(false);
      return;
    }

    setSuccessMessage("Password reset instructions have been sent.");
    setIsSubmitting(false);
  }

  return (
    <AuthFormCard
      eyebrow="Account recovery"
      title="Reset your password"
      description="Enter your account email and we will send a secure reset link."
      footerLabel="Remembered it?"
      footerHref="/login"
      footerAction="Return to sign in"
    >
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
        <AuthTextField
          label="Email"
          type="email"
          placeholder="you@example.com"
          autoComplete="email"
          registration={form.register("email")}
          error={form.formState.errors.email}
        />
        <FormError message={formError} />
        {successMessage ? <p className="text-sm text-success">{successMessage}</p> : null}
        <Button className="h-10 w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
          Send reset link
        </Button>
      </form>
    </AuthFormCard>
  );
}
