"use client";

import { WifiOff } from "lucide-react";
import { useEffect, useState } from "react";
import { logClientEvent } from "@/lib/client-logger";

export function RuntimeSafety() {
  const [online, setOnline] = useState(true);

  useEffect(() => {
    function handleOnline() {
      setOnline(true);
      logClientEvent({ level: "info", message: "Client connection restored" });
    }

    function handleOffline() {
      setOnline(false);
      logClientEvent({ level: "warn", message: "Client connection lost" });
    }

    function handleError(event: ErrorEvent) {
      logClientEvent({
        component: "window.error",
        error: event.error ?? event.message,
        level: "error",
        message: "Unhandled client runtime error",
      });
    }

    function handleRejection(event: PromiseRejectionEvent) {
      logClientEvent({
        component: "window.unhandledrejection",
        error: event.reason,
        level: "error",
        message: "Unhandled client promise rejection",
      });
    }

    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    window.addEventListener("error", handleError);
    window.addEventListener("unhandledrejection", handleRejection);

    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
      window.removeEventListener("error", handleError);
      window.removeEventListener("unhandledrejection", handleRejection);
    };
  }, []);

  if (online) {
    return null;
  }

  return (
    <div className="fixed inset-x-3 bottom-3 z-50 mx-auto flex max-w-md items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium text-foreground shadow-raised">
      <WifiOff className="size-4 shrink-0 text-primary" aria-hidden />
      <span className="min-w-0">You are offline. Cached views stay available where possible.</span>
    </div>
  );
}
