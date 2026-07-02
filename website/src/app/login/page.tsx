"use client";

import { useEffect } from "react";
import { PORTAL_URL } from "@/lib/siteConfig";

export default function LoginPage() {
  useEffect(() => {
    // Redirect to the actual web portal
    window.location.replace(PORTAL_URL);
  }, []);

  return (
    <div className="min-h-screen bg-[#FAF7F2] flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto mb-6" />
        <p className="text-neutral-600">Redirecting to portal...</p>
      </div>
    </div>
  );
}
