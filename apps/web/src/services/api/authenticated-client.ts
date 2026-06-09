"use client";

import { apiClient } from "@/services/api/client";
import { getSupabaseBrowserClient } from "@/features/auth/services/auth-client";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function authenticatedApiClient<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const supabase = getSupabaseBrowserClient();
  const {
    data: { session },
  } = await supabase.auth.getSession();

  const headers = new Headers(options.headers);
  if (session?.access_token) {
    headers.set("Authorization", `Bearer ${session.access_token}`);
  }

  return apiClient<T>(path, { ...options, headers });
}
