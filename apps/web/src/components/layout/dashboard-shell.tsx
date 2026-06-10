"use client";

import {
  BarChart3,
  Bell,
  Home,
  FileUp,
  Landmark,
  LifeBuoy,
  ReceiptText,
  Sparkles,
  UserRound,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Skeleton } from "@/components/ui/skeleton";
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
  { href: "/notifications", label: "Alerts", icon: Bell },
  { href: "/imports", label: "Import", icon: FileUp },
  { href: "/accounts", label: "Accounts", icon: Landmark },
  { href: "/dashboard", label: "Profile", icon: UserRound },
] as const;

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

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,color-mix(in_oklch,var(--primary),white_93%)_0%,var(--background)_26rem)]">
      <div className="grid min-h-screen lg:grid-cols-[17rem_1fr]">
        <aside className="sticky top-0 hidden h-screen border-r border-border/70 bg-card/72 px-4 py-5 backdrop-blur lg:block">
          <div className="flex h-full flex-col">
            <Link href="/dashboard" className="flex items-center gap-3 px-2">
              <span className="grid size-9 place-items-center rounded-lg bg-primary text-primary-foreground">
                <Sparkles className="size-4" aria-hidden />
              </span>
              <span>
                <span className="block text-sm font-semibold">SpendSense</span>
                <span className="block text-xs text-muted-foreground">Private money OS</span>
              </span>
            </Link>

            <nav className="mt-8 grid gap-1">
              {navItems.map((item, index) => {
                const Icon = item.icon;
                const active =
                  item.href === "/accounts"
                    ? pathname === "/accounts"
                    : item.href === "/imports"
                      ? pathname.startsWith("/imports")
                      : item.href === "/insights"
                        ? pathname.startsWith("/insights")
                        : item.href === "/notifications"
                          ? pathname.startsWith("/notifications")
                          : index === 0 && pathname === "/dashboard";
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
          </div>
        </aside>

        <div className="flex min-w-0 flex-col pb-20 lg:pb-0">
          <header className="sticky top-0 z-20 border-b border-border/70 bg-background/82 backdrop-blur">
            <div className="flex min-h-16 items-center justify-between gap-3 px-4 sm:px-6 lg:px-8">
              <div className="flex min-w-0 items-center gap-3">
                <div className="min-w-0">
                  <p className="truncate text-xs font-medium text-muted-foreground">Protected workspace</p>
                  {profileQuery.isLoading ? (
                    <Skeleton className="mt-1 h-5 w-40" />
                  ) : (
                    <h1 className="truncate text-lg font-semibold">Good to see you, {displayName}</h1>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Link
                  href="/notifications"
                  className="relative grid size-10 place-items-center rounded-lg border border-border bg-card text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                  title="Notifications"
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

          <main className="container-dashboard w-full py-5 sm:py-7">{children}</main>
        </div>
      </div>

      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-card/94 px-3 py-2 backdrop-blur lg:hidden">
        <div className="mx-auto grid max-w-xl grid-cols-7 gap-1">
          {navItems.map((item, index) => {
            const Icon = item.icon;
            const active =
              item.href === "/accounts"
                ? pathname === "/accounts"
                : item.href === "/imports"
                  ? pathname.startsWith("/imports")
                  : item.href === "/insights"
                    ? pathname.startsWith("/insights")
                    : item.href === "/notifications"
                      ? pathname.startsWith("/notifications")
                      : index === 0 && pathname === "/dashboard";
            return (
              <Link
                key={`${item.label}-mobile-${index}`}
                href={item.href}
                className={cn(
                  "grid h-12 place-items-center rounded-lg text-xs font-medium transition-colors",
                  active ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted"
                )}
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
