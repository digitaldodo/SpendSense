"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { AuthContext } from "@/features/auth/hooks/use-auth";
import {
  getE2eAuthSession,
  getSupabaseBrowserClient,
  type AuthSession,
} from "@/features/auth/services/auth-client";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => getE2eAuthSession());
  const [isLoading, setIsLoading] = useState(() => !getE2eAuthSession());
  const queryClient = useQueryClient();

  useEffect(() => {
    const supabase = getSupabaseBrowserClient();

    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setIsLoading(false);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      setSession(nextSession);
      setIsLoading(false);
      void queryClient.invalidateQueries();
    });

    return () => subscription.unsubscribe();
  }, [queryClient]);

  const value = useMemo(
    () => ({
      session,
      user: session?.user ?? null,
      isLoading,
      async signOut() {
        const supabase = getSupabaseBrowserClient();
        await supabase.auth.signOut();
        setSession(null);
        queryClient.clear();
      },
    }),
    [isLoading, queryClient, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
