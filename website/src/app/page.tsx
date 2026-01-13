"use client";

import { motion, useScroll, useTransform } from "framer-motion";
import Link from "next/link";
import Image from "next/image";
import { useRef } from "react";

export default function HomePage() {
  return (
    <div className="bg-[#FAF7F2]">
      {/* Hero Section */}
      <HeroSection />
      
      {/* Platform Screenshots Gallery */}
      <ScreenshotShowcase />
      
      {/* Features Bento Grid */}
      <FeaturesSection />
      
      {/* Social Proof / Stats */}
      <StatsSection />
      
      {/* Platform Portals */}
      <PortalsSection />
      
      {/* Compliance Highlight */}
      <ComplianceSection />
      
      {/* CTA Section */}
      <CTASection />
    </div>
  );
}

/* ===== Hero Section ===== */
function HeroSection() {
  return (
    <section className="relative pt-32 lg:pt-40 pb-20 lg:pb-32 overflow-hidden text-neutral-900">
      {/* Subtle radial gradient */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_50%_at_50%_-20%,rgba(4,120,87,0.12),transparent)]" />
      
      <div className="container-tight relative z-10">
        <div className="max-w-4xl">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="inline-flex items-center gap-2 px-4 py-2 bg-[#047857]/10 rounded-full text-sm font-medium text-[#047857] mb-8"
          >
            <span className="w-2 h-2 rounded-full bg-[#047857] animate-pulse" />
            Regulatory-Grade Compliance Platform
          </motion.div>

          {/* Massive Headline - Anthropic Style */}
          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.1 }}
            className="text-balance mb-8 text-neutral-900"
          >
            The{" "}
            <span className="relative inline-block">
              <span className="relative z-10">operating system</span>
              <span className="absolute bottom-2 left-0 w-full h-3 bg-[#047857]/20 -z-0" />
            </span>
            {" "}for biomedical waste compliance
          </motion.h1>

          {/* Subheadline */}
          <motion.p
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.2 }}
            className="text-xl lg:text-2xl text-[#525252] max-w-2xl mb-10 leading-relaxed"
          >
            Automate logistics, eliminate revenue leakage, and ensure 100% CPCB compliance. 
            Built for modern CBWTFs and healthcare facilities.
          </motion.p>

          {/* CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.3 }}
            className="flex flex-col sm:flex-row items-start sm:items-center gap-4"
          >
            <Link
              href="/contact"
              className="inline-flex px-8 py-4 bg-emerald-700 hover:bg-emerald-800 text-white font-semibold rounded-lg transition-all shadow-lg hover:shadow-xl"
            >
              Request a Demo
            </Link>
            <Link
              href="/platform"
              className="inline-flex px-8 py-4 text-neutral-900 font-semibold bg-neutral-100 hover:bg-neutral-200 rounded-lg transition-all items-center gap-2"
            >
              Explore Platform
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </motion.div>
        </div>


        {/* Hero Screenshot */}
        <motion.div
          initial={{ opacity: 0, y: 60 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.4 }}
          className="mt-16 lg:mt-24"
        >
          <div className="screenshot-frame">
            <Image
              src="/screenshots/platform-1.png"
              alt="SmartCBWTF Dashboard"
              width={1400}
              height={800}
              className="rounded-lg"
              priority
            />
          </div>
        </motion.div>
      </div>
    </section>
  );
}

/* ===== Screenshot Showcase ===== */
function ScreenshotShowcase() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start end", "end start"]
  });
  
  const x1 = useTransform(scrollYProgress, [0, 1], [0, -100]);
  const x2 = useTransform(scrollYProgress, [0, 1], [0, 100]);

  return (
    <section ref={containerRef} className="py-20 lg:py-32 overflow-hidden bg-[#f5f0e8]">
      <div className="container-tight mb-16 text-center">
        <motion.p
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
        >
          Platform Overview
        </motion.p>
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-balance"
        >
          One platform for complete visibility
        </motion.h2>
      </div>
      
      {/* Scrolling screenshot rows */}
      <div className="space-y-6">
        <motion.div style={{ x: x1 }} className="flex gap-6 pl-6">
          {[3, 5, 10, 15, 21].map((num) => (
            <div key={num} className="flex-shrink-0 w-[400px] lg:w-[500px]">
              <div className="screenshot-frame">
                <Image
                  src={`/screenshots/platform-${num}.png`}
                  alt={`Platform screenshot ${num}`}
                  width={500}
                  height={300}
                  className="rounded-lg"
                />
              </div>
            </div>
          ))}
        </motion.div>
        
        <motion.div style={{ x: x2 }} className="flex gap-6 -ml-32">
          {[2, 7, 12, 18, 25].map((num) => (
            <div key={num} className="flex-shrink-0 w-[400px] lg:w-[500px]">
              <div className="screenshot-frame">
                <Image
                  src={`/screenshots/platform-${num}.png`}
                  alt={`Platform screenshot ${num}`}
                  width={500}
                  height={300}
                  className="rounded-lg"
                />
              </div>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

/* ===== Features Bento Grid ===== */
function FeaturesSection() {
  return (
    <section className="section-spacing">
      <div className="container-tight">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
          >
            Core Capabilities
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-balance mb-6"
          >
            Built for operational excellence
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="text-lg text-[#525252]"
          >
            Every feature is designed for CPCB compliance, financial accountability, and audit-ready operations.
          </motion.p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <BentoCard
            title="Revenue Protection"
            description="Reconcile waste collection against billing. Track every bag from pickup to processing eliminating revenue leakage."
            icon={<ChartIcon />}
            color="sage"
            className="lg:col-span-2"
            image="/screenshots/platform-5.png"
          />
          <BentoCard
            title="Live GPS Tracking"
            description="Real-time vehicle tracking with geofenced pickup verification and route optimization."
            icon={<MapIcon />}
            color="sky"
            image="/screenshots/platform-10.png"
          />
          <BentoCard
            title="QR Traceability"
            description="Complete chain of custody with QR code verification at every handoff point."
            icon={<QRIcon />}
            color="amber"
          />
          <BentoCard
            title="Compliance Reports"
            description="Auto-generate Form-IV, monthly summaries, and annual reports. Always audit-ready."
            icon={<DocumentIcon />}
            color="rose"
            image="/screenshots/platform-25.png"
          />
          <BentoCard
            title="Route Planning"
            description="Optimize collection routes, assign staff, and manage daily operations in one workflow."
            icon={<RouteIcon />}
            color="sage"
            image="/screenshots/platform-21.png"
          />
        </div>
      </div>
    </section>
  );
}

/* ===== Bento Card Component ===== */
function BentoCard({
  title,
  description,
  icon,
  color,
  className = "",
  image,
}: {
  title: string;
  description: string;
  icon: React.ReactNode;
  color: "sage" | "sky" | "amber" | "rose";
  className?: string;
  image?: string;
}) {
  const colorClasses = {
    sage: "bento-sage",
    sky: "bento-sky",
    amber: "bento-amber",
    rose: "bento-rose",
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      className={`${colorClasses[color]} rounded-2xl p-6 lg:p-8 transition-all hover:shadow-lg group ${className}`}
    >
      <div className="flex flex-col h-full">
        <div className="w-12 h-12 rounded-xl bg-white/80 flex items-center justify-center mb-5 shadow-sm">
          {icon}
        </div>
        <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">{title}</h3>
        <p className="text-[#525252] leading-relaxed mb-6">{description}</p>
        
        {image && (
          <div className="mt-auto -mx-6 -mb-6 lg:-mx-8 lg:-mb-8">
            <div className="screenshot-frame rounded-t-none">
              <Image
                src={image}
                alt={title}
                width={600}
                height={350}
                className="rounded-lg rounded-t-none group-hover:scale-[1.02] transition-transform duration-500"
              />
            </div>
          </div>
        )}
      </div>
    </motion.div>
  );
}

/* ===== Stats Section ===== */
function StatsSection() {
  const stats = [
    { value: "100%", label: "Audit Trail Integrity" },
    { value: "24/7", label: "GPS Monitoring" },
    { value: "CPCB", label: "Fully Compliant" },
    { value: "0", label: "Revenue Leakage" },
  ];

  return (
    <section className="py-20 lg:py-28 bg-[#1a1a1a] text-white">
      <div className="container-tight">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-12">
          {stats.map((stat, i) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="text-center"
            >
              <p className="text-4xl lg:text-5xl font-bold text-[#047857] mb-2">{stat.value}</p>
              <p className="text-sm text-neutral-400">{stat.label}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ===== Portals Section ===== */
function PortalsSection() {
  const portals = [
    {
      title: "CBWTF Administrators",
      description: "Complete operational control over routes, HCFs, billing, staff, and compliance reporting.",
      features: ["Dashboard & Analytics", "Route Planning", "HCF Management", "Billing & Invoicing"],
      image: "/screenshots/platform-1.png",
    },
    {
      title: "HCF Administrators",
      description: "Self-service portal for healthcare facilities to track waste, generate labels, and stay compliant.",
      features: ["Waste Tracking", "QR Labels", "Compliance Reports", "Agreement & Dues"],
      image: "/screenshots/platform-25.png",
    },
    {
      title: "Management",
      description: "Executive oversight with approval workflows, financial dashboards, and compliance verification.",
      features: ["Approval Workflows", "Financial Oversight", "Audit Reports", "Multi-facility View"],
      image: "/screenshots/platform-3.png",
    },
  ];

  return (
    <section className="section-spacing bg-[#f5f0e8]">
      <div className="container-tight">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4"
          >
            Role-Based Access
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            Designed for every stakeholder
          </motion.h2>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {portals.map((portal, i) => (
            <motion.div
              key={portal.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="bg-white rounded-2xl overflow-hidden shadow-md hover:shadow-xl transition-shadow"
            >
              <div className="aspect-video relative overflow-hidden">
                <Image
                  src={portal.image}
                  alt={portal.title}
                  fill
                  className="object-cover object-top"
                />
              </div>
              <div className="p-6">
                <h3 className="text-xl font-semibold text-[#1a1a1a] mb-3">{portal.title}</h3>
                <p className="text-[#525252] text-sm mb-5">{portal.description}</p>
                <div className="flex flex-wrap gap-2">
                  {portal.features.map((f) => (
                    <span key={f} className="px-3 py-1 bg-[#f5f0e8] text-xs font-medium text-[#525252] rounded-full">
                      {f}
                    </span>
                  ))}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ===== Compliance Section ===== */
function ComplianceSection() {
  return (
    <section className="section-spacing">
      <div className="container-tight">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-4">
              Compliance First
            </p>
            <h2 className="mb-6">
              Built for CPCB regulations
            </h2>
            <p className="text-lg text-[#525252] mb-8 leading-relaxed">
              Every feature is designed from the ground up to meet Bio-Medical Waste Management Rules. 
              Complete audit trails, immutable records, and auto-generated compliance reports.
            </p>
            <ul className="space-y-4">
              {[
                "Day-wise waste tracking with immutable records",
                "QR code traceability from source to disposal",
                "Monthly & annual Form-IV report generation",
                "Role-based access with complete audit trails",
                "Dues-gated report access for accountability",
              ].map((item, i) => (
                <motion.li
                  key={i}
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className="flex items-start gap-3"
                >
                  <span className="w-5 h-5 rounded-full bg-[#047857] flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span className="text-[#1a1a1a]">{item}</span>
                </motion.li>
              ))}
            </ul>
            <Link
              href="/compliance"
              className="inline-flex items-center gap-2 mt-8 text-[#047857] font-semibold group"
            >
              Learn about compliance
              <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </motion.div>
          
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <div className="screenshot-frame">
              <Image
                src="/screenshots/platform-25.png"
                alt="Compliance Reports"
                width={600}
                height={400}
                className="rounded-lg"
              />
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}

/* ===== CTA Section ===== */
function CTASection() {
  return (
    <section className="py-24 lg:py-32 bg-emerald-700 text-white relative overflow-hidden">
      {/* Decorative gradient */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_50%_at_50%_120%,rgba(255,255,255,0.1),transparent)]" />
      
      <div className="container-tight relative z-10 text-center">
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="!text-white text-balance mb-6"
          style={{ color: 'white' }}
        >
          Ready to modernize your operations?
        </motion.h2>
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.1 }}
          className="text-xl max-w-2xl mx-auto mb-10"
          style={{ color: 'rgba(255, 255, 255, 0.9)' }}
        >
          Join leading CBWTFs who trust SmartCBWTF for regulatory compliance and operational excellence.
        </motion.p>
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.2 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <Link
            href="/contact"
            className="inline-flex px-8 py-4 bg-white text-emerald-700 font-semibold rounded-lg transition-all hover:bg-emerald-50 shadow-lg"
          >
            Schedule a Demo
          </Link>
          <Link
            href="/login"
            className="inline-flex px-8 py-4 text-white font-semibold border-2 border-white rounded-lg transition-all hover:bg-white hover:text-emerald-700"
          >
            Log in to Portal
          </Link>
        </motion.div>
      </div>
    </section>
  );
}


/* ===== Icons ===== */
function ChartIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
    </svg>
  );
}

function MapIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  );
}

function QRIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h2M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
    </svg>
  );
}

function DocumentIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  );
}

function RouteIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
    </svg>
  );
}
