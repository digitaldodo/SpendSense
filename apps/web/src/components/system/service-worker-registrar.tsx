"use client";

import { useEffect } from "react";
import { env } from "@/config/env";
import { logClientEvent } from "@/lib/client-logger";

export function ServiceWorkerRegistrar() {
  useEffect(() => {
    if (!("serviceWorker" in navigator) || env.NEXT_PUBLIC_APP_ENV === "local") {
      return;
    }

    navigator.serviceWorker
      .register("/sw.js", { scope: "/" })
      .then((registration) => {
        logClientEvent({
          component: "service-worker",
          context: { scope: registration.scope },
          level: "info",
          message: "Service worker registered",
        });
      })
      .catch((error: unknown) => {
        logClientEvent({
          component: "service-worker",
          error,
          level: "warn",
          message: "Service worker registration failed",
        });
      });
  }, []);

  return null;
}
