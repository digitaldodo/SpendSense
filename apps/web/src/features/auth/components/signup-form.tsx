"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormError } from "@/components/feedback/form-error";
import { AuthFormCard } from "@/features/auth/components/auth-form-card";
import { AuthTextField } from "@/features/auth/components/auth-text-field";
import {
  signupSchema,
  type SignupFormValues,
} from "@/features/auth/schemas/auth-schemas";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";

export function SignupForm() {
  const router = useRouter();
  const [formError, setFormError] = useState<string>();
  const [successMessage, setSuccessMessage] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const form = useForm<SignupFormValues>({
    mode: "onBlur",
    resolver: zodResolver(signupSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: SignupFormValues) {
    setIsSubmitting(true);
    setFormError(undefined);
    setSuccessMessage(undefined);

    const supabase = getSupabaseBrowserClient();
    const { data, error } = await supabase.auth.signUp({
      ...values,
      options: {
        emailRedirectTo: `${window.location.origin}/onboarding`,
      },
    });

    if (error) {
      setFormError(error.message);
      setIsSubmitting(false);
      return;
    }

    if (data.session) {
      router.replace("/onboarding");
      router.refresh();
      return;
    }

    setSuccessMessage("Check your inbox to confirm your email, then sign in.");
    setIsSubmitting(false);
  }

  return (
    <AuthFormCard
      eyebrow="Private by design"
      title="Create your account"
      description="Start with a protected profile. Product workflows arrive in later phases."
      footerLabel="Already have access?"
      footerHref="/login"
      footerAction="Sign in"
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
        <AuthTextField
          label="Password"
          type="password"
          placeholder="At least 8 characters"
          autoComplete="new-password"
          registration={form.register("password")}
          error={form.formState.errors.password}
        />
        <FormError message={formError} />
        {successMessage ? <p className="text-sm text-success">{successMessage}</p> : null}
        <Button className="h-10 w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
          Create account
        </Button>
      </form>
    </AuthFormCard>
  );
}
