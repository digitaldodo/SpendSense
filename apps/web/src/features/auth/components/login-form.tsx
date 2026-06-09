"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { FormError } from "@/components/feedback/form-error";
import { AuthFormCard } from "@/features/auth/components/auth-form-card";
import { AuthTextField } from "@/features/auth/components/auth-text-field";
import {
  loginSchema,
  type LoginFormValues,
} from "@/features/auth/schemas/auth-schemas";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [formError, setFormError] = useState<string>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const redirectTo = sanitizeRedirect(searchParams.get("redirectTo"));
  const form = useForm<LoginFormValues>({
    mode: "onBlur",
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    setIsSubmitting(true);
    setFormError(undefined);

    const supabase = getSupabaseBrowserClient();
    const { error } = await supabase.auth.signInWithPassword(values);

    if (error) {
      setFormError(error.message);
      setIsSubmitting(false);
      return;
    }

    router.replace(redirectTo);
    router.refresh();
  }

  return (
    <AuthFormCard
      eyebrow="Secure access"
      title="Welcome back"
      description="Sign in to continue to your protected SpendSense workspace."
      footerLabel="New to SpendSense?"
      footerHref="/signup"
      footerAction="Create an account"
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
        <div className="space-y-2">
          <AuthTextField
            label="Password"
            type="password"
            placeholder="Enter your password"
            autoComplete="current-password"
            registration={form.register("password")}
            error={form.formState.errors.password}
          />
          <div className="text-right">
            <Link className="text-sm font-medium text-primary hover:text-primary/80" href="/forgot-password">
              Forgot password?
            </Link>
          </div>
        </div>
        <FormError message={formError} />
        <Button className="h-10 w-full" type="submit" disabled={isSubmitting}>
          {isSubmitting ? <Loader2 className="size-4 animate-spin" aria-hidden /> : null}
          Sign in
        </Button>
      </form>
    </AuthFormCard>
  );
}

function sanitizeRedirect(value: string | null) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/dashboard";
  }
  return value;
}
