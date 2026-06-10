import type { Metadata } from "next";
import { OperationalBanner } from "@/components/system/operational-banner";
import { AppProviders } from "@/providers/app-providers";
import "./globals.css";

const siteUrl = new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://spendsense.app");
const appEnv = process.env.NEXT_PUBLIC_APP_ENV ?? "local";
const allowSearchIndexing = appEnv === "production" || appEnv === "local" || appEnv === "development";

export const metadata: Metadata = {
  metadataBase: siteUrl,
  title: {
    default: "SpendSense",
    template: "%s | SpendSense",
  },
  description:
    "SpendSense gives households a calm, secure workspace for imports, spending visibility, budgets, goals, and delivery-ready financial operations.",
  applicationName: "SpendSense",
  alternates: {
    canonical: "/",
  },
  appleWebApp: {
    capable: true,
    title: "SpendSense",
    statusBarStyle: "default",
  },
  category: "finance",
  icons: {
    icon: "/icons/icon.svg",
    apple: "/icons/icon.svg",
  },
  manifest: "/manifest.webmanifest",
  openGraph: {
    type: "website",
    url: siteUrl,
    siteName: "SpendSense",
    title: "SpendSense",
    description:
      "A secure, app-like finance workspace for spending visibility, imports, budgets, goals, and operational reliability.",
    images: [
      {
        url: "/social-preview.svg",
        width: 1200,
        height: 630,
        alt: "SpendSense dashboard preview",
      },
    ],
  },
  robots: {
    index: allowSearchIndexing,
    follow: allowSearchIndexing,
  },
  twitter: {
    card: "summary_large_image",
    title: "SpendSense",
    description:
      "A secure, app-like finance workspace for spending visibility, imports, budgets, goals, and operational reliability.",
    images: ["/social-preview.svg"],
  },
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className="h-full antialiased"
      data-scroll-behavior="smooth"
      suppressHydrationWarning
    >
      <body className="flex min-h-full flex-col">
        <OperationalBanner />
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
