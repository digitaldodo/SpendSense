"use client";

import {
  BarChart3,
  Bell,
  Home,
  FileUp,
  Landmark,
  LifeBuoy,
  ReceiptText,
  ShieldCheck,
  Sparkles,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { SpendSenseLogo } from "@/components/brand/spendsense-logo";
import { Skeleton } from "@/components/ui/skeleton";
import { env } from "@/config/env";
import { LogoutButton } from "@/features/auth/components/logout-button";
import { NotificationToastHost } from "@/features/notifications/components/notification-toast-host";
import { useNotificationSummary } from "@/features/notifications/hooks/use-notifications";
import { ProfileRouteGuard } from "@/features/profile/components/profile-route-guard";
import { useProfile } from "@/features/profile/hooks/use-profile";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Home", icon: Home },
  { href: "/dashboard#transactions", label: "Transactions", icon: ReceiptText },
  { href: "/insights", label: "Insights", icon: BarChart3 },
  { href: "/mentor", label: "Mentor", icon: Sparkles },
  { href: "/notifications", label: "Alerts", icon: Bell },
  { href: "/imports", label: "Import", icon: FileUp },
  { href: "/accounts", label: "Accounts", icon: Landmark },
  { href: "/admin/operations", label: "Admin", icon: ShieldCheck, adminOnly: true },
  { href: "/dashboard", label: "Profile", icon: UserRound },
] as const;

const mobileNavItems = navItems.filter((item) =>
  ["Home", "Insights", "Mentor", "Import", "Alerts"].includes(item.label)
);

export function DashboardShell({ children }: { children: React.ReactNode }) {
  return (
    <ProfileRouteGuard requireOnboardingComplete>
      <DashboardShellContent>{children}</DashboardShellContent>
    </ProfileRouteGuard>
  );
}

function DashboardShellContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const profileQuery = useProfile();
  const notificationSummaryQuery = useNotificationSummary();
  const profile = profileQuery.data;
  const displayName = profile?.displayName || profile?.email?.split("@")[0] || "there";
  const unreadCount = notificationSummaryQuery.data?.unreadCount ?? 0;

  function isNavActive(item: (typeof navItems)[number], index: number) {
    if (item.href === "/accounts") {
      return pathname === "/accounts";
    }
    if (item.href === "/admin/operations") {
      return pathname.startsWith("/admin");
    }
    if (item.href === "/imports") {
      return pathname.startsWith("/imports");
    }
    if (item.href === "/insights") {
      return pathname.startsWith("/insights");
    }
    if (item.href === "/mentor") {
      return pathname.startsWith("/mentor");
    }
    if (item.href === "/notifications") {
      return pathname.startsWith("/notifications");
    }
    return index === 0 && pathname === "/dashboard";
  }

  useEffect(() => {
    function focusSkipLink(event: KeyboardEvent) {
      if (event.key !== "Tab" || event.shiftKey) {
        return;
      }
      const activeElement = document.activeElement;
      const startsAtDocument =
        activeElement === document.body || activeElement === document.documentElement;
      if (!startsAtDocument) {
        return;
      }
      const skipLink = document.querySelector<HTMLAnchorElement>("[data-skip-link]");
      skipLink?.focus();
      event.preventDefault();
    }

    window.addEventListener("keydown", focusSkipLink, true);
    return () => window.removeEventListener("keydown", focusSkipLink, true);
  }, []);

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,color-mix(in_oklch,var(--primary),white_93%)_0%,var(--background)_26rem)]">
      <a
        data-skip-link
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-primary-foreground"
      >
        Skip to content
      </a>
      <div className="grid min-h-screen lg:grid-cols-[17rem_1fr]">
        <aside className="sticky top-0 hidden h-screen border-r border-border/70 bg-card/72 px-4 py-5 backdrop-blur lg:block">
          <div className="flex h-full flex-col">
            <Link
              href="/dashboard"
              className="group/logo flex items-center rounded-lg px-2 py-1 transition-colors hover:bg-muted/55"
              aria-label="SpendSense dashboard"
            >
              <SpendSenseLogo size="sm" subtitle="Private money OS" priority />
            </Link>

            <nav className="mt-8 grid gap-1" aria-label="Primary">
              {navItems
                .filter((item) => !("adminOnly" in item) || profile?.roles.includes("ADMIN"))
                .map((item, index) => {
                  const Icon = item.icon;
                  const active = isNavActive(item, index);
                  return (
                    <Link
                      key={`${item.label}-${index}`}
                      href={item.href}
                      className={cn(
                        "flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium transition-colors",
                        active
                          ? "bg-primary text-primary-foreground"
                          : "text-muted-foreground hover:bg-muted hover:text-foreground"
                      )}
                      aria-current={active ? "page" : undefined}
                    >
                      <Icon className="size-4" aria-hidden />
                      {item.label}
                    </Link>
                  );
                })}
            </nav>

            <div className="mt-auto rounded-lg border border-border bg-muted/45 p-4">
              <div className="flex items-start gap-3">
                <LifeBuoy className="mt-0.5 size-4 text-primary" aria-hidden />
                <div className="space-y-1">
                  <p className="text-sm font-medium">Foundation ready</p>
                  <p className="text-xs leading-5 text-muted-foreground">
                    Account and transaction foundations are ready for secure ingestion paths.
                  </p>
                </div>
              </div>
            </div>
            <div className="mt-3 rounded-lg border border-border/70 bg-background/55 p-3 text-xs text-muted-foreground">
              <div className="flex items-center justify-between gap-3">
                <span className="font-medium uppercase tracking-normal">
                  {env.NEXT_PUBLIC_APP_ENV}
                </span>
                <span className="font-mono">{env.NEXT_PUBLIC_APP_VERSION}</span>
              </div>
              <p className="mt-1 truncate font-mono">{env.NEXT_PUBLIC_RELEASE_COMMIT}</p>
            </div>
          </div>
        </aside>

        <div className="flex min-w-0 flex-col pb-20 lg:pb-0">
          <header className="sticky top-0 z-20 border-b border-border/70 bg-background/82 backdrop-blur">
            <div className="flex min-h-16 items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
              <div className="flex min-w-0 items-center gap-3">
                <Link
                  href="/dashboard"
                  className="group/logo -ml-1 grid size-10 place-items-center rounded-lg transition-colors hover:bg-muted lg:hidden"
                  aria-label="SpendSense dashboard"
                >
                  <SpendSenseLogo variant="mark" size="sm" />
                </Link>
                <div className="min-w-0">
                  <p className="truncate text-xs font-medium text-muted-foreground">
                    Protected workspace
                  </p>
                  {profileQuery.isLoading ? (
                    <Skeleton className="mt-1 h-5 w-40" />
                  ) : (
                    <h1 className="truncate text-lg font-semibold">
                      Good to see you, {displayName}
                    </h1>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Link
                  href="/notifications"
                  className="relative grid size-10 place-items-center rounded-lg border border-border bg-card text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  title="Notifications"
                  aria-label={
                    unreadCount > 0 ? `${unreadCount} unread notifications` : "Notifications"
                  }
                >
                  <Bell className="size-4" aria-hidden />
                  {unreadCount > 0 ? (
                    <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-primary px-1 text-[0.65rem] font-semibold text-primary-foreground">
                      {unreadCount > 9 ? "9+" : unreadCount}
                    </span>
                  ) : null}
                </Link>
                <LogoutButton />
              </div>
            </div>
          </header>

          <main id="main-content" className="container-dashboard w-full py-5 sm:py-7">
            {children}
          </main>
        </div>
      </div>

      <nav
        className="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-card/94 px-3 py-2 backdrop-blur lg:hidden"
        aria-label="Mobile primary"
      >
        <div className="mx-auto grid max-w-2xl grid-cols-[repeat(auto-fit,minmax(3.75rem,1fr))] gap-1">
          {mobileNavItems
            .filter((item) => !("adminOnly" in item) || profile?.roles.includes("ADMIN"))
            .map((item, index) => {
              const Icon = item.icon;
              const active = isNavActive(item, index);
              return (
                <Link
                  key={`${item.label}-mobile-${index}`}
                  href={item.href}
                  className={cn(
                    "grid h-12 place-items-center rounded-lg text-xs font-medium transition-colors",
                    active ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted"
                  )}
                  aria-current={active ? "page" : undefined}
                >
                  <Icon className="size-4" aria-hidden />
                  <span className="truncate">{item.label}</span>
                </Link>
              );
            })}
        </div>
      </nav>
      <NotificationToastHost />
    </div>
  );
}
