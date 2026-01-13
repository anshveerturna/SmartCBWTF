"use client";

import { motion } from "framer-motion";
import Link from "next/link";

const complianceAreas = [
  {
    title: "Day-wise Waste Tracking",
    description: "Complete logging of waste generation by category (Red, Yellow, White, Blue) with timestamps and QR verification.",
    items: ["Category-based segregation", "Weight verification at pickup", "Immutable timestamp records", "HCF-level granularity"],
  },
  {
    title: "Chain of Custody",
    description: "Full traceability from waste generation point to final treatment. Every handoff logged and verified.",
    items: ["QR code at every stage", "GPS-verified pickups", "Digital signatures", "Photo documentation"],
  },
  {
    title: "Form-IV Reporting",
    description: "Automatic generation of CPCB-mandated reports with complete data integrity and audit trails.",
    items: ["Monthly summaries", "Annual reports", "HCF-specific breakdowns", "Export to PDF/Excel"],
  },
  {
    title: "Audit Trail Integrity",
    description: "Immutable logs of every action taken in the system. No retroactive modifications allowed.",
    items: ["Tamper-proof records", "User action logging", "Timestamp verification", "Regulatory compliance"],
  },
  {
    title: "Role-Based Access Control",
    description: "Strict separation of duties with role-specific portals and permissions.",
    items: ["CBWTF Admin access", "HCF restricted views", "Management oversight", "Super Admin controls"],
  },
  {
    title: "Dues-Gated Compliance",
    description: "Report access tied to payment status. Ensures financial accountability alongside regulatory compliance.",
    items: ["Outstanding dues alerts", "Report access controls", "Payment verification", "Automated reminders"],
  },
];

export default function CompliancePage() {
  return (
    <div className="bg-[#FAF7F2]">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-20">
        <div className="container-tight">
          <div className="max-w-3xl">
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
              className="text-balance mb-6"
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
        </div>
      </section>

      {/* Compliance Areas */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {complianceAreas.map((area, i) => (
              <motion.div
                key={area.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="bg-white rounded-2xl p-8 shadow-sm"
              >
                <div className="w-10 h-10 rounded-lg bg-[#047857]/10 flex items-center justify-center mb-5">
                  <svg className="w-5 h-5 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                </div>
                <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">{area.title}</h3>
                <p className="text-[#525252] mb-5">{area.description}</p>
                <ul className="grid grid-cols-2 gap-2">
                  {area.items.map((item) => (
                    <li key={item} className="flex items-center gap-2 text-sm text-[#525252]">
                      <svg className="w-4 h-4 text-[#047857] flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="mb-6">Aligned with regulatory frameworks</h2>
            <p className="text-lg text-[#525252] mb-12">
              SmartCBWTF is designed to ensure compliance with all applicable biomedical waste management regulations.
            </p>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                { title: "CPCB Guidelines", desc: "Central Pollution Control Board standards" },
                { title: "SPCB Requirements", desc: "State-level reporting compliance" },
                { title: "BMW Rules 2016", desc: "Bio-Medical Waste Management Rules" },
              ].map((reg, i) => (
                <motion.div
                  key={reg.title}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="bg-[#f5f0e8] rounded-xl p-6"
                >
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{reg.title}</h3>
                  <p className="text-sm text-[#525252]">{reg.desc}</p>
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
          <h2 className="text-white mb-6">Need compliance guidance?</h2>
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
