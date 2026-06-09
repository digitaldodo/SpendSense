"use client";

import { LogOut } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/use-auth";

export function LogoutButton() {
  const router = useRouter();
  const { signOut } = useAuth();
  const [isSigningOut, setIsSigningOut] = useState(false);

  async function handleLogout() {
    setIsSigningOut(true);
    await signOut();
    router.replace("/login");
    router.refresh();
  }

  return (
    <Button variant="outline" className="gap-2" onClick={handleLogout} disabled={isSigningOut}>
      <LogOut className="size-4" aria-hidden />
      Sign out
    </Button>
  );
}
