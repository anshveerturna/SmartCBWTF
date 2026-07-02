const DEFAULT_SITE_URL = "https://smartcbwtf.com";
const DEFAULT_PORTAL_URL = "https://portal.smartcbwtf.com";

const cleanUrl = (value: string | undefined, fallback: string, envName: string) => {
  const configured = (value || fallback).trim().replace(/\/+$/, "");
  const parsed = new URL(configured);
  if (process.env.NODE_ENV === "production" && parsed.protocol !== "https:") {
    throw new Error(`${envName} must use HTTPS in production builds.`);
  }
  return parsed.toString().replace(/\/+$/, "");
};

export const SITE_URL = cleanUrl(process.env.NEXT_PUBLIC_SITE_URL, DEFAULT_SITE_URL, "NEXT_PUBLIC_SITE_URL");
export const PORTAL_URL = cleanUrl(process.env.NEXT_PUBLIC_PORTAL_URL, DEFAULT_PORTAL_URL, "NEXT_PUBLIC_PORTAL_URL");

