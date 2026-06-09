"use client";

import type { Session, SupabaseClient, User } from "@supabase/supabase-js";
import { createBrowserSupabaseClient } from "@/lib/supabase/client";

let supabaseClient: SupabaseClient | null = null;

export function getSupabaseBrowserClient() {
  if (!supabaseClient) {
    supabaseClient = createBrowserSupabaseClient();
  }
  return supabaseClient;
}

export type AuthSession = Session;
export type AuthUser = User;
