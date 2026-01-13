"use client";

import { motion } from "framer-motion";
import Image from "next/image";
import Link from "next/link";

const complianceAreas = [
  {
    title: "Day-wise Waste Tracking",
    description: "Complete logging of waste generation by category (Red, Yellow, White, Blue) with timestamps and QR verification.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
      </svg>
    ),
    items: ["Category-based segregation", "Weight verification at pickup", "Immutable timestamp records", "HCF-level granularity"],
  },
  {
    title: "Chain of Custody",
    description: "Full traceability from generation to treatment. Every handoff is digitally logged and verified.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
      </svg>
    ),
    items: ["QR code at every stage", "GPS-verified pickups", "Digital signatures", "Photo documentation"],
  },
  {
    title: "Form-IV Reporting",
    description: "Auto-generation of CPCB-mandated annual reports with complete data integrity and audit trails.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    ),
    items: ["Monthly summaries", "Annual reports", "HCF-specific breakdowns", "Export to PDF/Excel"],
  },
  {
    title: "Audit Trail Integrity",
    description: "Immutable logs of every action taken in the system. No retroactive modifications allowed.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
      </svg>
    ),
    items: ["Tamper-proof records", "User action logging", "Timestamp verification", "Regulatory compliance"],
  },
  {
    title: "Role-Based Access",
    description: "Strict separation of duties with role-specific portals and permissions for secure operations.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
    items: ["CBWTF Admin access", "HCF restricted views", "Management oversight", "Super Admin controls"],
  },
  {
    title: "Dues-Gated Compliance",
    description: "Report access tied to payment status. Ensures financial accountability alongside compliance.",
    icon: (
      <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
    items: ["Outstanding dues alerts", "Report access controls", "Payment verification", "Automated reminders"],
  },
];

export default function CompliancePage() {
  return (
    <div className="bg-[#FAF7F2] text-neutral-900">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-20 overflow-hidden">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
              >
                Compliance
              </motion.p>
              <motion.h1
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="text-balance mb-6 text-neutral-900"
              >
                Built for CPCB regulations
              </motion.h1>
              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-xl text-[#525252] leading-relaxed"
              >
                Every feature in SmartCBWTF is designed from the ground up to meet Bio-Medical Waste Management Rules, 2016. Complete audit trails, immutable records, and auto-generated compliance documentation.
              </motion.p>
            </div>
            <motion.div
              initial={{ opacity: 0, x: 20, rotate: -2 }}
              animate={{ opacity: 1, x: 0, rotate: 0 }}
              transition={{ delay: 0.3, duration: 0.8 }}
              className="flex justify-center relative"
            >
              <div className="absolute inset-0 bg-[#047857]/20 blur-3xl rounded-full -z-10 w-3/4 h-3/4 m-auto" />
              <Image
                src="/compliance-hero-3d.png"
                alt="3D Compliance illustration"
                width={600}
                height={600}
                className="w-full max-w-lg drop-shadow-2xl"
              />
            </motion.div>
          </div>
        </div>
      </section>

      {/* Compliance Areas */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {complianceAreas.map((area, i) => (
              <motion.div
                key={area.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                whileHover={{ y: -4, boxShadow: "0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)" }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="bg-white rounded-2xl p-8 shadow-sm border-t-4 border-[#047857]"
              >
                <div className="w-12 h-12 rounded-xl bg-[#047857]/10 flex items-center justify-center mb-6 text-[#047857]">
                  {area.icon}
                </div>
                <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">{area.title}</h3>
                <p className="text-[#525252] mb-6 text-sm leading-relaxed">{area.description}</p>
                <ul className="space-y-3">
                  {area.items.map((item) => (
                    <li key={item} className="flex items-start gap-2.5 text-sm text-[#525252]">
                      <svg className="w-5 h-5 text-[#047857] flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      {item}
                    </li>
                  ))}
                </ul>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Regulatory Alignment */}
      <section className="section-spacing">
        <div className="container-tight">
          <div className="max-w-4xl mx-auto text-center">
            <h2 className="mb-6 text-neutral-900">Aligned with regulatory frameworks</h2>
            <p className="text-lg text-[#525252] mb-12 max-w-2xl mx-auto">
              SmartCBWTF is designed to ensure compliance with all applicable biomedical waste management regulations.
            </p>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {[
                { 
                  title: "CPCB Guidelines", 
                  desc: "Central Pollution Control Board standards",
                  icon: (
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  )
                },
                { 
                  title: "SPCB Requirements", 
                  desc: "State-level reporting compliance",
                  icon: (
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                    </svg>
                  )
                },
                { 
                  title: "BMW Rules 2016", 
                  desc: "Bio-Medical Waste Management Rules",
                  icon: (
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  )
                },
              ].map((reg, i) => (
                <motion.div
                  key={reg.title}
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="group flex flex-col items-center p-6 rounded-2xl bg-white border border-neutral-200 hover:border-[#047857]/30 transition-colors"
                >
                  <div className="w-12 h-12 rounded-full bg-[#FAF7F2] flex items-center justify-center text-[#047857] mb-4 group-hover:scale-110 transition-transform">
                    {reg.icon}
                  </div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{reg.title}</h3>
                  <p className="text-sm text-[#525252] leading-relaxed">{reg.desc}</p>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Disclaimer + CTA */}
      <section className="py-20 bg-[#1a1a1a] text-white">
        <div className="container-tight text-center">
          <p className="text-sm text-neutral-500 mb-8 max-w-2xl mx-auto">
            This information is provided for general awareness. Consult with regulatory authorities for specific compliance requirements applicable to your facility.
          </p>
          <h2 className="text-white mb-6" style={{ color: 'white' }}>Need compliance guidance?</h2>
          <p className="text-neutral-400 mb-8 max-w-xl mx-auto">
            Our team can help you understand how SmartCBWTF ensures regulatory compliance for your operations.
          </p>
          <Link
            href="/contact"
            className="inline-flex px-8 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all"
          >
            Talk to an Expert
          </Link>
        </div>
      </section>
    </div>
  );
}
