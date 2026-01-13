"use client";

import { motion } from "framer-motion";
import Image from "next/image";

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
                className="text-balance mb-6 text-neutral-900 font-bold"
              >
                Bank-grade security standards
              </motion.h1>
              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-xl text-[#525252] leading-relaxed"
              >
                We protect sensitive healthcare data with a defense-in-depth strategy. From AWS infrastructure to encryption at rest, every layer is fortified.
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
        <div className="container-tight space-y-20">
          
          {/* Infrastructure */}
          <div>
            <h2 className="text-2xl font-bold text-[#1a1a1a] mb-8 border-b border-[#e7e5e4] pb-4">Infrastructure</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <SecurityCard 
                title="AWS Hosting" 
                desc="Hosted on Amazon Web Services for enterprise-grade reliability and 99.9% uptime."
                delay={0}
                icon={
                  <div className="relative w-8 h-8">
                    <Image 
                      src="/aws-logo.png" 
                      alt="AWS Logo" 
                      fill
                      className="object-contain"
                    />
                  </div>
                }
              />
              <SecurityCard 
                title="Auto-Scaling" 
                desc="Dynamically scales compute resources to gracefully handle traffic spikes."
                delay={0.1}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                }
              />
              <SecurityCard 
                title="Multi-Region Backup" 
                desc="Automated backups replicated across geographic regions for disaster recovery."
                delay={0.2}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                }
              />
              <SecurityCard 
                title="DDoS Protection" 
                desc="AWS Shield Advanced provides always-on detection and automatic inline mitigations."
                delay={0.3}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                }
              />
            </div>
          </div>

          {/* Data Protection */}
          <div>
            <h2 className="text-2xl font-bold text-[#1a1a1a] mb-8 border-b border-[#e7e5e4] pb-4">Data Protection</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <SecurityCard 
                title="TLS 1.3 Encryption" 
                desc="Data in transit is protected using TLS 1.3, ensuring privacy and data integrity."
                delay={0}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                }
              />
              <SecurityCard 
                title="AES-256 at Rest" 
                desc="All stored data is encrypted using the industry-standard AES-256 algorithm."
                delay={0.1}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
                  </svg>
                }
              />
              <SecurityCard 
                title="Key Management" 
                desc="Strict cryptographic key management using AWS KMS with regular rotation policies."
                delay={0.2}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                  </svg>
                }
              />
              <SecurityCard 
                title="Data Isolation" 
                desc="Logical separation of tenant data ensures that your data remains strictly private."
                delay={0.3}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                }
              />
            </div>
          </div>

          {/* Access Control */}
          <div>
            <h2 className="text-2xl font-bold text-[#1a1a1a] mb-8 border-b border-[#e7e5e4] pb-4">Access Control</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <SecurityCard 
                title="Role-Based Permissions" 
                desc="Granular RBAC ensures users only access resources necessary for their role."
                delay={0}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0c0 .883-.393 1.627-1 1.948M10 6L9 6" />
                  </svg>
                }
              />
              <SecurityCard 
                title="JWT Authentication" 
                desc="Secure, stateless authentication using JSON Web Tokens (JWT) for all API requests."
                delay={0.1}
                icon={
                  <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
                  </svg>
                }
              />
            </div>
          </div>

        </div>
      </section>

      {/* Trust Section */}
      <section className="section-spacing">
        <div className="container-tight text-center">
          <h2 className="mb-6 text-neutral-900">Security transparently verified</h2>
          <p className="text-lg text-[#525252] max-w-2xl mx-auto mb-12">
            We believe trust is earned through transparency. Our security posture is open for review.
          </p>
          
          <div className="grid grid-cols-3 gap-8 max-w-xl mx-auto">
            {[
              { value: "256-bit", label: "Encryption Standard" },
              { value: "99.9%", label: "Uptime Guaranteed" },
              { value: "ISO", label: "Aligned Controls" },
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

function SecurityCard({ title, desc, icon, delay }: { title: string, desc: string, icon: React.ReactNode, delay: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay }}
      className="group flex flex-col p-6 rounded-xl bg-white border border-neutral-200 hover:border-[#047857]/30 transition-colors"
    >
      <div className="w-10 h-10 rounded-lg bg-[#FAF7F2] flex items-center justify-center mb-4 group-hover:scale-105 transition-transform">
        {icon}
      </div>
      <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{title}</h3>
      <p className="text-sm text-[#525252] leading-relaxed">{desc}</p>
    </motion.div>
  );
}
