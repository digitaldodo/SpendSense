import { Suspense } from "react";
import { LoginForm } from "@/features/auth/components/login-form";

export const metadata = {
  title: "Sign In",
};

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="h-80 rounded-lg border border-border bg-card" />}>
      <LoginForm />
    </Suspense>
  );
}
