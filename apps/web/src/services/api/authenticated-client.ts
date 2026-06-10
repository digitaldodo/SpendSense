"use client";

import { apiClient } from "@/services/api/client";
import { getE2eAuthSession, getSupabaseBrowserClient } from "@/features/auth/services/auth-client";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
};

export async function authenticatedApiClient<T>(
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const e2eSession = getE2eAuthSession();
  const session =
    e2eSession ??
    (isLocalE2eRequest()
      ? null
      : (await getSupabaseBrowserClient().auth.getSession()).data.session);

  const headers = new Headers(options.headers);
  if (session?.access_token || isLocalE2eRequest()) {
    headers.set("Authorization", `Bearer ${session?.access_token ?? "e2e-token"}`);
  }

  return apiClient<T>(path, { ...options, headers });
}

function isLocalE2eRequest() {
  return (
    typeof window !== "undefined" &&
    ["localhost", "127.0.0.1"].includes(window.location.hostname) &&
    document.cookie.includes("__spendsense_e2e_session=1")
  );
}
