"use client";

import { createContext, useContext } from "react";
import type { AuthSession, AuthUser } from "@/features/auth/services/auth-client";

export type AuthContextValue = {
  session: AuthSession | null;
  user: AuthUser | null;
  isLoading: boolean;
  signOut: () => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }
  return context;
}
