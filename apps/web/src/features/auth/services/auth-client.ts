"use client";

import type { Session, SupabaseClient, User } from "@supabase/supabase-js";
import { createBrowserSupabaseClient } from "@/lib/supabase/client";

let supabaseClient: SupabaseClient | null = null;

export function getSupabaseBrowserClient() {
  const e2eClient = getE2eSupabaseClient();
  if (e2eClient) {
    return e2eClient;
  }
  if (!supabaseClient) {
    supabaseClient = createBrowserSupabaseClient();
  }
  return supabaseClient;
}

export type AuthSession = Session;
export type AuthUser = User;

export function getE2eAuthSession() {
  if (
    typeof window === "undefined" ||
    !["localhost", "127.0.0.1"].includes(window.location.hostname)
  ) {
    return null;
  }
  const rawSession = window.localStorage.getItem("__SPENDSENSE_E2E_SESSION__");
  if (rawSession) {
    return JSON.parse(rawSession) as Session;
  }
  if (document.cookie.includes("__spendsense_e2e_session=1")) {
    return {
      access_token: "e2e-token",
      refresh_token: "e2e-refresh",
      token_type: "bearer",
      expires_in: 3600,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
      user: {
        id: "20000000-0000-4000-8000-000000000001",
        email: "demo@spendsense.local",
        app_metadata: {},
        user_metadata: {},
        aud: "authenticated",
        created_at: new Date().toISOString(),
      },
    } as Session;
  }
  return null;
}

function getE2eSupabaseClient() {
  const session = getE2eAuthSession();
  if (!session) {
    return null;
  }
  return {
    auth: {
      async getSession() {
        return { data: { session }, error: null };
      },
      onAuthStateChange(callback: (_event: string, session: Session | null) => void) {
        callback("SIGNED_IN", session);
        return {
          data: {
            subscription: {
              unsubscribe() {},
            },
          },
        };
      },
      async signOut() {
        window.localStorage.removeItem("__SPENDSENSE_E2E_SESSION__");
        return { error: null };
      },
    },
  } as unknown as SupabaseClient;
}
