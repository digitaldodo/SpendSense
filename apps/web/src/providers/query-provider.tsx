"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            gcTime: 10 * 60_000,
            refetchOnWindowFocus: false,
            refetchOnReconnect: "always",
            retry: (failureCount, error) => {
              const status =
                typeof error === "object" && error && "status" in error ? Number(error.status) : 0;
              return status >= 500 && failureCount < 2;
            },
            staleTime: 60_000,
          },
          mutations: {
            retry: 0,
          },
        },
      })
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
