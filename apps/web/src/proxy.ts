import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { updateSession } from "@/lib/supabase/middleware";

export function proxy(request: NextRequest) {
  const requestHost = request.headers.get("host") ?? request.nextUrl.host;
  const localhostRequest =
    requestHost.startsWith("localhost") || requestHost.startsWith("127.0.0.1");
  const lighthouseBypass = localhostRequest && request.nextUrl.searchParams.get("lhci") === "1";
  const nonProductionApp = process.env.NEXT_PUBLIC_APP_ENV !== "production";
  const explicitCiBypass =
    process.env.NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS === "1" ||
    process.env.SPENDSENSE_ENABLE_CI_MOCK_API === "1";
  const ciBypassEnabled = explicitCiBypass || request.cookies.has("__spendsense_e2e_session");

  if (lighthouseBypass || explicitCiBypass || (nonProductionApp && ciBypassEnabled)) {
    return NextResponse.next();
  }

  return updateSession(request);
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)",
  ],
};
