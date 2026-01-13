"use client";

import { motion } from "framer-motion";
import Link from "next/link";
import Image from "next/image";

export default function PlatformPage() {
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
              Platform
            </motion.p>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-6"
            >
              End-to-end compliance infrastructure
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252] leading-relaxed"
            >
              SmartCBWTF connects every stakeholder in the biomedical waste chain—from healthcare facilities to treatment plants—with role-based portals, GPS tracking, and immutable audit trails.
            </motion.p>
          </div>
        </div>
      </section>

      {/* Data Flow Architecture */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="mb-4">How it works</h2>
            <p className="text-[#525252] text-lg">
              A seamless flow from waste generation to compliant disposal
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-5 gap-6">
            {[
              { step: "1", title: "Waste Generated", desc: "HCF logs daily waste by category with QR labels" },
              { step: "2", title: "Pickup Scheduled", desc: "CBWTF assigns routes and vehicles" },
              { step: "3", title: "GPS Verified", desc: "Real-time tracking confirms collection" },
              { step: "4", title: "Processed", desc: "Weight reconciled, invoice generated" },
              { step: "5", title: "Reported", desc: "Auto-generated Form-IV compliance" },
            ].map((item, i) => (
              <motion.div
                key={item.step}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="bg-white rounded-2xl p-6 text-center shadow-sm"
              >
                <span className="inline-flex items-center justify-center w-10 h-10 rounded-full bg-[#047857] text-white font-bold text-lg mb-4">
                  {item.step}
                </span>
                <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{item.title}</h3>
                <p className="text-sm text-[#525252]">{item.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Role-based Portals */}
      <section className="section-spacing">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <div>
              <p className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4">
                Role-Based Access
              </p>
              <h2 className="mb-6">Three portals, one platform</h2>
              <p className="text-lg text-[#525252] mb-8">
                Each stakeholder sees only what they need. Strict role separation ensures data security and operational clarity.
              </p>
              
              <div className="space-y-6">
                {[
                  { title: "CBWTF Admin Portal", desc: "Full operational control—routes, HCFs, billing, staff, compliance" },
                  { title: "HCF Admin Portal", desc: "Self-service waste tracking, labels, reports, consumable orders" },
                  { title: "Management Portal", desc: "Approval workflows, financial oversight, multi-facility analytics" },
                ].map((portal, i) => (
                  <motion.div
                    key={portal.title}
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: i * 0.1 }}
                    className="border-l-4 border-[#047857] pl-6"
                  >
                    <h3 className="text-lg font-semibold text-[#1a1a1a] mb-1">{portal.title}</h3>
                    <p className="text-[#525252]">{portal.desc}</p>
                  </motion.div>
                ))}
              </div>
            </div>
            
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <div className="screenshot-frame">
                <Image
                  src="/screenshots/platform-1.png"
                  alt="CBWTF Dashboard"
                  width={600}
                  height={400}
                  className="rounded-lg"
                />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Technical Foundation */}
      <section className="py-20 lg:py-28 bg-[#1a1a1a] text-white">
        <div className="container-tight">
          <div className="max-w-3xl mx-auto text-center mb-16">
            <h2 className="text-white mb-4">Enterprise-grade foundation</h2>
            <p className="text-neutral-400 text-lg">
              Built with security, scalability, and compliance at its core
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              { title: "AWS Infrastructure", desc: "Hosted on AWS with 99.9% uptime SLA, auto-scaling, and redundancy" },
              { title: "End-to-End Encryption", desc: "TLS 1.3 in transit, AES-256 at rest. Zero-knowledge architecture" },
              { title: "Immutable Audit Logs", desc: "Every action timestamped and tamper-proof. Full CPCB compliance" },
            ].map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="bg-neutral-800 rounded-2xl p-6"
              >
                <div className="w-10 h-10 rounded-lg bg-[#047857] flex items-center justify-center mb-4">
                  <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-white mb-2">{feature.title}</h3>
                <p className="text-neutral-400">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="section-spacing">
        <div className="container-tight text-center">
          <h2 className="mb-6">See the platform in action</h2>
          <p className="text-lg text-[#525252] max-w-2xl mx-auto mb-10">
            Schedule a personalized demo to explore how SmartCBWTF can transform your operations.
          </p>
          <Link
            href="/contact"
            className="inline-flex px-8 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all shadow-lg shadow-[#047857]/25"
          >
            Request Demo
          </Link>
        </div>
      </section>
    </div>
  );
}
