import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";
import { SITE_URL } from "@/lib/siteConfig";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "SmartCBWTF | Regulatory-Grade Biomedical Waste Management Platform",
    template: "%s | SmartCBWTF",
  },
  description:
    "The Operating System for Bio-Medical Waste Compliance. Automate logistics, eliminate revenue leakage, and ensure 100% CPCB regulatory compliance. Built for modern CBWTFs.",
  keywords: [
    "biomedical waste management",
    "CBWTF software",
    "healthcare waste tracking",
    "CPCB compliance",
    "BMW Rules 2016",
    "QR code waste tracking",
    "medical waste logistics",
    "hospital waste management",
    "waste audit trail",
    "Form IV reporting",
  ],
  authors: [{ name: "SmartCBWTF" }],
  creator: "SmartCBWTF",
  publisher: "SmartCBWTF",
  formatDetection: {
    email: false,
    telephone: false,
  },
  openGraph: {
    title: "SmartCBWTF | Bio-Medical Waste Compliance Platform",
    description:
      "Enterprise-grade SaaS for biomedical waste logistics, compliance, and auditability. CPCB compliant. Built for CBWTFs.",
    url: SITE_URL,
    siteName: "SmartCBWTF",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "SmartCBWTF - Biomedical Waste Compliance Platform",
      },
    ],
    type: "website",
    locale: "en_IN",
  },
  twitter: {
    card: "summary_large_image",
    title: "SmartCBWTF | Bio-Medical Waste Compliance Platform",
    description:
      "Enterprise-grade SaaS for biomedical waste logistics and CPCB compliance.",
    images: ["/og-image.png"],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  verification: {
    // Add when available
    // google: "your-google-verification-code",
  },
  alternates: {
    canonical: SITE_URL,
  },
};

// JSON-LD Structured Data
const jsonLd = {
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  name: "SmartCBWTF",
  applicationCategory: "BusinessApplication",
  operatingSystem: "Web",
  description:
    "Regulatory-grade biomedical waste management platform for CBWTFs and healthcare facilities. CPCB compliant with complete audit trails.",
  offers: {
    "@type": "Offer",
    availability: "https://schema.org/InStock",
    priceCurrency: "INR",
  },
  aggregateRating: {
    "@type": "AggregateRating",
    ratingValue: "4.8",
    ratingCount: "50",
  },
  provider: {
    "@type": "Organization",
    name: "SmartCBWTF",
    url: SITE_URL,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <link rel="icon" href="/logo.svg" type="image/svg+xml" />
        <link rel="apple-touch-icon" href="/logo.svg" />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className={`${inter.variable} font-sans antialiased bg-white text-[#0f172a]`}>
        <Navbar />
        <main className="min-h-screen">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
