"use client";

import { useMemo, useState } from "react";
import { Bell, CheckCheck, X } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { useMarkNotificationRead, useNotificationSummary } from "@/features/notifications/hooks/use-notifications";
import { cn } from "@/lib/utils";

export function NotificationToastHost() {
  const summaryQuery = useNotificationSummary();
  const markRead = useMarkNotificationRead();
  const [dismissedId, setDismissedId] = useState<string | null>(null);
  const latestUnread = useMemo(
    () => summaryQuery.data?.latest.find((item) => !item.read && item.id !== dismissedId),
    [dismissedId, summaryQuery.data?.latest]
  );

  if (!latestUnread) {
    return null;
  }

  return (
    <div
      className={cn(
        "fixed bottom-20 right-4 z-40 w-[calc(100vw-2rem)] max-w-sm rounded-lg border border-border bg-card p-4 shadow-raised transition duration-300 lg:bottom-5",
        "translate-y-0 opacity-100"
      )}
    >
      <div className="flex items-start gap-3">
        <span className="grid size-9 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
          <Bell className="size-4" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold">{latestUnread.title}</p>
          <p className="mt-1 max-h-10 overflow-hidden text-sm text-muted-foreground">{latestUnread.body}</p>
          <div className="mt-3 flex flex-wrap gap-2">
            {latestUnread.actionUrl && latestUnread.actionLabel ? (
              <Button size="sm" variant="outline" render={<Link href={latestUnread.actionUrl} />}>
                {latestUnread.actionLabel}
              </Button>
            ) : null}
            <Button size="icon-sm" variant="ghost" title="Mark read" onClick={() => markRead.mutate(latestUnread.id)}>
              <CheckCheck className="size-4" aria-hidden />
            </Button>
          </div>
        </div>
        <button
          className="grid size-7 place-items-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          type="button"
          onClick={() => {
            setDismissedId(latestUnread.id);
          }}
          title="Dismiss toast"
        >
          <X className="size-4" aria-hidden />
        </button>
      </div>
    </div>
  );
}
