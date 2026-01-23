"use client";

import { motion } from "framer-motion";
import Link from "next/link";
import Image from "next/image";

export default function PlatformPage() {
  return (
    <div className="bg-[#FAF7F2] text-neutral-900">
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
              className="text-balance mb-6 text-neutral-900"
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

      {/* Platform Components Overview */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="mb-4 text-neutral-900">Platform components</h2>
            <p className="text-[#525252] text-lg">
              Web portals for administrators, Android app for field operations
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Web Portal */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="bg-white rounded-2xl p-8 shadow-sm"
            >
              <div className="w-12 h-12 rounded-lg bg-[#047857]/10 flex items-center justify-center mb-6">
                <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">Web Portal</h3>
              <p className="text-[#525252] mb-6">
                Full-featured dashboards for CBWTF administrators, HCF managers, and executive oversight. Real-time analytics, invoice management, and compliance reporting.
              </p>
              <div className="screenshot-frame">
                <Image
                  src="/screenshots/platform-1.webp"
                  alt="SmartCBWTF Web Dashboard"
                  width={500}
                  height={300}
                  className="rounded-lg"
                />
              </div>
            </motion.div>

            {/* Android App */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="bg-white rounded-2xl p-8 shadow-sm"
            >
              <div className="w-12 h-12 rounded-lg bg-[#047857]/10 flex items-center justify-center mb-6">
                <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">Android App</h3>
              <p className="text-[#525252] mb-6">
                Purpose-built for field staff. Offline-first architecture with QR scanning, Bluetooth scale integration, GPS geofencing, and WorkManager sync.
              </p>
              <div className="flex justify-center gap-4">
                <Image
                  src="/screenshots/app-2.webp"
                  alt="SmartCBWTF Android App - Dashboard"
                  width={150}
                  height={300}
                  className="rounded-2xl shadow-lg"
                />
                <Image
                  src="/screenshots/app-3.webp"
                  alt="SmartCBWTF Android App - Scan & Weigh"
                  width={150}
                  height={300}
                  className="rounded-2xl shadow-lg"
                />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Android App Features */}
      <section className="section-spacing">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <div>
              <p className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4">
                Field Operations
              </p>
              <h2 className="mb-6 text-neutral-900">Android app capabilities</h2>
              <p className="text-lg text-[#525252] mb-8">
                Built for drivers and plant operators. Our Android app enables waste collection, verification, and HCF registration—even without connectivity.
              </p>
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { title: "Pickup Waste", desc: "QR scan + Bluetooth weight capture at HCFs" },
                  { title: "Verify at CBWTF", desc: "Re-verify incoming bags at treatment facility" },
                  { title: "Register HCF", desc: "Onboard new healthcare facilities in-field" },
                  { title: "Mark Attendance", desc: "GPS-verified driver check-ins at locations" },
                  { title: "Offline Mode", desc: "Works without network, syncs when online" },
                  { title: "My Route", desc: "View assigned HCFs in optimized order" },
                ].map((feature, i) => (
                  <motion.div
                    key={feature.title}
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ delay: i * 0.05 }}
                    className="flex items-start gap-3"
                  >
                    <div className="w-5 h-5 rounded-full bg-[#047857] flex items-center justify-center flex-shrink-0 mt-0.5">
                      <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                    <div>
                      <h4 className="font-semibold text-[#1a1a1a]">{feature.title}</h4>
                      <p className="text-sm text-[#525252]">{feature.desc}</p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
            
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="flex justify-center"
            >
              <div className="relative">
                <Image
                  src="/screenshots/app-2.webp"
                  alt="SmartCBWTF Android App"
                  width={280}
                  height={560}
                  className="rounded-3xl shadow-2xl"
                />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Role-based Portals */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <div>
              <p className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4">
                Role-Based Access
              </p>
              <h2 className="mb-6 text-neutral-900">Three portals, one platform</h2>
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
                  src="/screenshots/platform-3.webp"
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
            <h2 className="!text-white mb-4 font-bold">Enterprise-grade foundation</h2>
            <p className="!text-neutral-300 text-lg">
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
                className="bg-neutral-800 rounded-2xl p-6 border border-neutral-700"
              >
                <div className="w-10 h-10 rounded-lg bg-[#047857] flex items-center justify-center mb-4">
                  <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold !text-white mb-2">{feature.title}</h3>
                <p className="!text-neutral-200">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="section-spacing">
        <div className="container-tight text-center">
          <h2 className="mb-6 text-neutral-900">See the platform in action</h2>
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
