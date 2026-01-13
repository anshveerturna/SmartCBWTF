"use client";

import { motion } from "framer-motion";
import Image from "next/image";
import Link from "next/link";

const portals = [
  {
    title: "CBWTF Admin Portal",
    description: "Complete operational control for facility administrators. Manage routes, HCFs, billing, staff, and compliance.",
    href: "https://app.smartcbwtf.com/login?portal=cbwtf",
    image: "/screenshots/platform-1.png",
    color: "#047857",
  },
  {
    title: "HCF Admin Portal",
    description: "Self-service portal for healthcare facility administrators. Track waste, generate labels, and manage compliance.",
    href: "https://app.smartcbwtf.com/login?portal=hcf",
    image: "/screenshots/platform-25.png",
    color: "#0369a1",
  },
  {
    title: "Top Management Portal",
    description: "Executive oversight and approval workflows. Monitor financials, verify compliance, and manage multi-facility operations.",
    href: "https://app.smartcbwtf.com/login?portal=management",
    image: "/screenshots/platform-3.png",
    color: "#7c3aed",
  },
];

export default function LoginPage() {
  return (
    <div className="bg-[#FAF7F2] min-h-screen">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-16">
        <div className="container-tight">
          <div className="text-center max-w-2xl mx-auto">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="mb-8"
            >
              <Image
                src="/logo.svg"
                alt="SmartCBWTF"
                width={64}
                height={64}
                className="mx-auto"
              />
            </motion.div>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-4"
            >
              Welcome back
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252]"
            >
              Select your portal to continue
            </motion.p>
          </div>
        </div>
      </section>

      {/* Portal Cards */}
      <section className="pb-24">
        <div className="container-tight">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
            {portals.map((portal, i) => (
              <motion.a
                key={portal.title}
                href={portal.href}
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 + i * 0.1 }}
                className="group bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300"
              >
                <div className="aspect-video relative overflow-hidden">
                  <Image
                    src={portal.image}
                    alt={portal.title}
                    fill
                    className="object-cover object-top group-hover:scale-105 transition-transform duration-500"
                  />
                  <div 
                    className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300"
                    style={{ background: `linear-gradient(to top, ${portal.color}20, transparent)` }}
                  />
                </div>
                <div className="p-6">
                  <div className="flex items-center gap-3 mb-3">
                    <div 
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: portal.color }}
                    />
                    <h3 className="text-lg font-semibold text-[#1a1a1a]">{portal.title}</h3>
                  </div>
                  <p className="text-sm text-[#525252] mb-4">{portal.description}</p>
                  <div 
                    className="inline-flex items-center gap-2 text-sm font-semibold group-hover:gap-3 transition-all"
                    style={{ color: portal.color }}
                  >
                    Continue to portal
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </div>
              </motion.a>
            ))}
          </div>

          {/* Help Text */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6 }}
            className="text-center mt-12"
          >
            <p className="text-[#525252]">
              Need help? Contact{" "}
              <a href="mailto:support@smartcbwtf.com" className="text-[#047857] hover:underline">
                support@smartcbwtf.com
              </a>
            </p>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
