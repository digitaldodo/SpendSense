"use client";

import { ThemeProvider } from "next-themes";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider } from "@/features/auth/components/auth-provider";
import { QueryProvider } from "@/providers/query-provider";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryProvider>
        <AuthProvider>
          <TooltipProvider delay={180}>{children}</TooltipProvider>
        </AuthProvider>
      </QueryProvider>
    </ThemeProvider>
  );
}
