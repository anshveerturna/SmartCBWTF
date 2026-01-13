"use client";

import { motion } from "framer-motion";
import Image from "next/image";

const securityFeatures = [
  {
    category: "Infrastructure",
    items: [
      { title: "AWS Hosting", desc: "Enterprise-grade cloud infrastructure with 99.9% uptime SLA" },
      { title: "Auto-Scaling", desc: "Dynamically scales to handle peak loads without performance degradation" },
      { title: "Multi-Region Backup", desc: "Regular backups with geographic redundancy for disaster recovery" },
      { title: "DDoS Protection", desc: "AWS Shield protects against distributed denial-of-service attacks" },
    ],
  },
  {
    category: "Data Protection",
    items: [
      { title: "TLS 1.3 Encryption", desc: "All data encrypted in transit with the latest TLS protocol" },
      { title: "AES-256 at Rest", desc: "Database and file storage encrypted with AES-256 standard" },
      { title: "Key Management", desc: "AWS KMS for secure key storage and rotation policies" },
      { title: "Data Isolation", desc: "Logical tenant separation ensures data privacy" },
    ],
  },
  {
    category: "Access Control",
    items: [
      { title: "Role-Based Permissions", desc: "Strict RBAC with principle of least privilege" },
      { title: "JWT Authentication", desc: "Stateless, secure token-based authentication" },
      { title: "Session Management", desc: "Configurable timeouts and single-session enforcement" },
      { title: "Account Lockout", desc: "Automatic lockout after failed login attempts" },
    ],
  },
  {
    category: "Audit & Compliance",
    items: [
      { title: "Immutable Audit Logs", desc: "Every action timestamped and logged without modification" },
      { title: "User Activity Tracking", desc: "Complete visibility into who did what and when" },
      { title: "Export Capabilities", desc: "Generate audit reports for regulatory submissions" },
      { title: "Tamper Detection", desc: "Cryptographic verification of log integrity" },
    ],
  },
];

export default function SecurityPage() {
  return (
    <div className="bg-[#FAF7F2] text-neutral-900">
      {/* Hero */}
      <section className="pt-32 lg:pt-40 pb-20">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
              >
                Security
              </motion.p>
              <motion.h1
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="text-balance mb-6 text-neutral-900"
              >
                Enterprise-grade security
              </motion.h1>
              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-xl text-[#525252] leading-relaxed"
              >
                Built with security at every layer. From infrastructure to application, SmartCBWTF protects sensitive healthcare and compliance data with industry-leading practices.
              </motion.p>
            </div>
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.3 }}
              className="flex justify-center"
            >
              <Image
                src="/security-hero.png"
                alt="Security illustration"
                width={400}
                height={400}
                className="w-full max-w-sm"
              />
            </motion.div>
          </div>
        </div>
      </section>

      {/* Security Features */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="space-y-16">
            {securityFeatures.map((section) => (
              <motion.div
                key={section.category}
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
              >
                <h2 className="text-2xl font-bold text-[#1a1a1a] mb-8 border-b border-[#e7e5e4] pb-4">
                  {section.category}
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {section.items.map((item, i) => (
                    <motion.div
                      key={item.title}
                      initial={{ opacity: 0, y: 20 }}
                      whileInView={{ opacity: 1, y: 0 }}
                      viewport={{ once: true }}
                      transition={{ delay: i * 0.05 }}
                      className="bg-white rounded-xl p-6 shadow-sm"
                    >
                      <div className="flex items-start gap-4">
                        <div className="w-10 h-10 rounded-lg bg-[#047857]/10 flex items-center justify-center flex-shrink-0">
                          <svg className="w-5 h-5 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                          </svg>
                        </div>
                        <div>
                          <h3 className="text-lg font-semibold text-[#1a1a1a] mb-1">{item.title}</h3>
                          <p className="text-[#525252]">{item.desc}</p>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Trust Section */}
      <section className="section-spacing">
        <div className="container-tight text-center">
          <h2 className="mb-6 text-neutral-900">Security you can trust</h2>
          <p className="text-lg text-[#525252] max-w-2xl mx-auto mb-12">
            We take security seriously. Our platform is continuously monitored and updated to protect against emerging threats.
          </p>
          
          <div className="grid grid-cols-3 gap-8 max-w-xl mx-auto">
            {[
              { value: "256-bit", label: "Encryption" },
              { value: "99.9%", label: "Uptime SLA" },
              { value: "24/7", label: "Monitoring" },
            ].map((stat) => (
              <div key={stat.label}>
                <p className="text-3xl font-bold text-[#047857] mb-1">{stat.value}</p>
                <p className="text-sm text-[#525252]">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
