"use client";

import { motion } from "framer-motion";
import { useState } from "react";
import Image from "next/image";

const roles = ["CBWTF Admin", "HCF Admin", "Management", "Android App"];

const features = {
  "CBWTF Admin": [
    { title: "Dashboard & Analytics", desc: "Real-time KPIs, waste trends, revenue tracking, and risk alerts", image: "/screenshots/platform-1.png" },
    { title: "Waste Analytics", desc: "Category breakdown, collection trends, and HCF contribution analysis", image: "/screenshots/platform-5.png" },
    { title: "Vehicle Tracking", desc: "Live GPS map with geofencing, offline alerts, and route history", image: "/screenshots/platform-10.png" },
    { title: "HCF Management", desc: "Onboard facilities, manage agreements, track dues, and verify compliance", image: "/screenshots/platform-15.png" },
    { title: "Route Planning", desc: "Create optimized routes, assign vehicles and staff, manage schedules", image: "/screenshots/platform-21.png" },
    { title: "Billing & Invoicing", desc: "Auto-generate invoices, track payments, reconcile with collections", image: "/screenshots/platform-7.png" },
    { title: "Staff Management", desc: "Assign roles, track attendance, manage credentials and permissions", image: "/screenshots/platform-8.png" },
    { title: "Compliance Reports", desc: "Generate Form-IV, monthly summaries, and audit-ready documentation", image: "/screenshots/platform-25.png" },
  ],
  "HCF Admin": [
    { title: "Dashboard", desc: "Track waste submissions, pending pickups, and outstanding dues", image: "/screenshots/platform-3.png" },
    { title: "Daily Waste Entry", desc: "Log waste by category with QR verification and weight tracking", image: "/screenshots/platform-25.png" },
    { title: "QR Label Generation", desc: "Print trackable labels for every waste bag with category coding", image: "/screenshots/platform-9.png" },
    { title: "Compliance Reports", desc: "View pickup history, generate HCF-specific compliance reports", image: "/screenshots/platform-25.png" },
    { title: "Agreement & Dues", desc: "View contract terms, track invoices, and payment history", image: "/screenshots/platform-12.png" },
    { title: "Order Consumables", desc: "Request waste bags, labels, and other supplies from CBWTF", image: "/screenshots/platform-6.png" },
  ],
  "Management": [
    { title: "Executive Dashboard", desc: "Multi-facility overview with key performance metrics", image: "/screenshots/platform-3.png" },
    { title: "Approval Workflows", desc: "Review and approve dues adjustments, write-offs, and exceptions", image: "/screenshots/platform-22.png" },
    { title: "Financial Oversight", desc: "Revenue reconciliation, outstanding dues, and collection efficiency", image: "/screenshots/platform-7.png" },
    { title: "Audit Verification", desc: "Verify compliance reports and review immutable audit trails", image: "/screenshots/platform-25.png" },
  ],
  "Android App": [
    { title: "Pickup Waste", desc: "Scan QR codes and capture weight via Bluetooth scale integration", image: "/screenshots/app-3.png" },
    { title: "Secure Login", desc: "Biometric and credential-based authentication for authorized staff access", image: "/screenshots/app-secure-login.png" },
    { title: "Verify at CBWTF", desc: "Verify waste receipt at the facility with automated reconciliation", image: "/screenshots/app-verify-cbwtf.png" },
    { title: "My Route", desc: "View assigned HCFs in optimized order for efficient collection", image: "/screenshots/app-my-route.png" },
    { title: "HCF Registration", desc: "Onboard new healthcare facilities directly from the field", image: "/screenshots/app-hcf-registration.png" },
    { title: "Attendance", desc: "Geofenced driver chart-in/out for accurate shift tracking", image: "/screenshots/app-attendance.png" },
  ],
};

export default function FeaturesPage() {
  const [activeRole, setActiveRole] = useState("CBWTF Admin");

  return (
    <div className="bg-[#FAF7F2]">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-16">
        <div className="container-tight">
          <div className="max-w-3xl">
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
            >
              Features
            </motion.p>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-6"
            >
              Everything you need, nothing you don't
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252] leading-relaxed"
            >
              Purpose-built tools for every role in the biomedical waste ecosystem. From operational control to regulatory compliance.
            </motion.p>
          </div>
        </div>
      </section>

      {/* Role Tabs */}
      <section className="pb-20">
        <div className="container-tight">
          {/* Tab Buttons */}
          <div className="flex flex-wrap gap-2 mb-12 p-1.5 bg-[#f5f0e8] rounded-xl w-fit">
            {roles.map((role) => (
              <button
                key={role}
                onClick={() => setActiveRole(role)}
                className={`px-6 py-3 text-sm font-semibold rounded-lg transition-all ${
                  activeRole === role
                    ? "bg-white text-[#1a1a1a] shadow-sm"
                    : "text-[#525252] hover:text-[#1a1a1a]"
                }`}
              >
                {role}
              </button>
            ))}
          </div>

          {/* Features Grid */}
          <motion.div
            key={activeRole}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className={`grid gap-6 ${
              activeRole === "Android App" 
                ? "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3" 
                : "grid-cols-1 md:grid-cols-2"
            }`}
          >
            {features[activeRole as keyof typeof features].map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-lg transition-shadow group flex flex-col"
              >
                <div className={`relative overflow-hidden bg-[#1a1a1a] ${
                  activeRole === "Android App" ? "aspect-[9/16]" : "aspect-video"
                }`}>
                  <Image
                    src={feature.image}
                    alt={feature.title}
                    fill
                    className={`object-cover ${
                      activeRole === "Android App" ? "object-top" : "object-top group-hover:scale-105 transition-transform duration-500"
                    }`}
                  />
                </div>
                <div className="p-6 flex-1">
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{feature.title}</h3>
                  <p className="text-[#525252]">{feature.desc}</p>
                </div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* All Features CTA */}
      <section className="py-20 bg-[#f5f0e8]">
        <div className="container-tight text-center">
          <h2 className="mb-6">Ready to explore?</h2>
          <p className="text-lg text-[#525252] max-w-2xl mx-auto mb-10">
            See all features in action with a personalized demo tailored to your facility's needs.
          </p>
          <a
            href="/contact"
            className="inline-flex px-8 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all shadow-lg shadow-[#047857]/25"
          >
            Schedule Demo
          </a>
        </div>
      </section>
    </div>
  );
}
