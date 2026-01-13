"use client";

import Image from "next/image";
import Link from "next/link";
import { motion, useScroll, useTransform } from "framer-motion";
import { useRef } from "react";

export default function Home() {
  return (
    <main className="overflow-hidden">
      <HeroSection />
      <TrustedSection />
      <ScreenshotGallery />
      <FeaturesSection />
      <HowItWorksSection />
      <AndroidAppSection />
      <PortalsSection />
      <StatsSection />
      <CTASection />
    </main>
  );
}

import TrustedClients from "@/components/TrustedClients";

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
          transition={{ duration: 0.8, delay: 0.4 }}
          className="mt-16 lg:mt-24"
        >
          <div className="screenshot-frame max-w-5xl mx-auto">
            <Image
              src="/screenshots/platform-1.png"
              alt="SmartCBWTF Dashboard"
              width={1200}
              height={700}
              className="rounded-lg"
              priority
            />
          </div>
        </motion.div>
      </div>
    </section>
  );
}

function TrustedSection() {
  return <TrustedClients />;
}

/* ===== Screenshot Gallery ===== */
function ScreenshotGallery() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start end", "end start"]
  });
  
  const x1 = useTransform(scrollYProgress, [0, 1], [0, -200]);
  const x2 = useTransform(scrollYProgress, [0, 1], [-100, 100]);

  return (
    <section ref={containerRef} className="py-20 lg:py-28 overflow-hidden bg-[#f5f0e8] text-neutral-900">
      <div className="container-tight text-center mb-12">
        <motion.p
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-3"
        >
          Platform Overview
        </motion.p>
        <motion.h2
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-balance text-neutral-900"
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
          {[7, 12, 18, 25, 28].map((num) => (
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

/* ===== Features Section (Bento Grid) ===== */
function FeaturesSection() {
  const features = [
    {
      title: "Waste Analytics",
      desc: "Track waste by category, weight, and source. Real-time dashboards with trend analysis.",
      icon: <ChartIcon />,
      bg: "bento-sage",
      image: "/screenshots/platform-5.png",
    },
    {
      title: "Live GPS Tracking",
      desc: "Real-time vehicle tracking with geofenced pickup verification and route optimization.",
      icon: <GpsIcon />,
      bg: "bento-sky",
      image: "/screenshots/platform-9.png",
    },
    {
      title: "QR Traceability",
      desc: "Complete chain of custody with QR code verification at every handoff point.",
      icon: <QrIcon />,
      bg: "bento-amber",
      image: "/screenshots/platform-18.png",
    },
    {
      title: "Compliance Reports",
      desc: "Auto-generate Form-IV, monthly summaries, and annual reports. Always audit-ready.",
      icon: <ReportIcon />,
      bg: "bento-sky",
      image: "/screenshots/platform-22.png",
    },
    {
      title: "Route Planning",
      desc: "Optimize collection routes, assign staff, and manage daily operations in one workflow.",
      icon: <RouteIcon />,
      bg: "bento-rose",
      image: "/screenshots/platform-21.png",
    },
  ];

  return (
    <section className="section-spacing text-neutral-900">
      <div className="container-tight">
        <div className="text-center mb-16">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-3"
          >
            Features
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-neutral-900"
          >
            Everything you need to stay compliant
          </motion.h2>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className={`${feature.bg} rounded-2xl p-6 ${i === 0 ? "lg:col-span-2 lg:row-span-2" : ""}`}
            >
              <div className="flex flex-col h-full">
                <div className="w-12 h-12 rounded-xl bg-white/80 flex items-center justify-center mb-4 shadow-sm">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-[#1a1a1a] mb-2">{feature.title}</h3>
                <p className="text-[#525252] mb-6">{feature.desc}</p>
                {(i === 0 || feature.image) && (
                  <div className="mt-auto">
                    <div className="screenshot-frame">
                      <Image
                        src={feature.image}
                        alt={feature.title}
                        width={600}
                        height={400}
                        className="rounded-lg"
                      />
                    </div>
                  </div>
                )}
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ===== How It Works Section ===== */
function HowItWorksSection() {
  const steps = [
    {
      num: 1,
      title: "Waste Generated",
      desc: "HCF logs daily waste by category with QR labels",
      icon: (
        <svg className="w-8 h-8 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      ),
    },
    {
      num: 2,
      title: "Pickup Scheduled",
      desc: "CBWTF assigns routes and vehicles automatically",
      icon: (
        <svg className="w-8 h-8 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      ),
    },
    {
      num: 3,
      title: "GPS Verified",
      desc: "Real-time tracking confirms collection at HCF",
      icon: (
        <svg className="w-8 h-8 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      ),
    },
    {
      num: 4,
      title: "Weighed & Verified",
      desc: "Bluetooth scale capture, no manual entry",
      icon: (
        <svg className="w-8 h-8 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
        </svg>
      ),
    },
    {
      num: 5,
      title: "Reported",
      desc: "Auto-generated Form-IV compliance reports",
      icon: (
        <svg className="w-8 h-8 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
  ];

  return (
    <section className="section-spacing bg-[#f5f0e8] text-neutral-900">
      <div className="container-tight">
        <div className="text-center mb-16">
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-neutral-900"
          >
            How it works
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
            className="text-lg text-[#525252] mt-4"
          >
            A seamless flow from waste generation to compliant disposal
          </motion.p>
        </div>

        {/* Steps with connecting line */}
        <div className="relative">
          {/* Connecting line (desktop) */}
          <motion.div 
            initial={{ width: 0 }}
            whileInView={{ width: "100%" }}
            viewport={{ once: true }}
            transition={{ duration: 1, ease: "easeInOut" }}
            className="hidden lg:block absolute top-1/2 left-0 h-0.5 bg-[#047857]/20 -translate-y-1/2" 
          />
          
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
            {steps.map((step, i) => (
              <motion.div
                key={step.title}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                whileHover={{ y: -8, boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)" }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="relative bg-white rounded-2xl p-6 text-center shadow-sm cursor-default border border-transparent hover:border-[#047857]/10"
              >
                {/* Step number */}
                <motion.div 
                  whileHover={{ scale: 1.1 }}
                  className="w-12 h-12 rounded-full bg-[#047857] text-white font-bold text-lg flex items-center justify-center mx-auto mb-4 relative z-10"
                >
                  {step.num}
                </motion.div>
                
                {/* Icon */}
                <div className="w-16 h-16 rounded-xl bg-[#047857]/10 flex items-center justify-center mx-auto mb-4 text-[#047857]">
                  {step.icon}
                </div>
                
                <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{step.title}</h3>
                <p className="text-sm text-[#525252]">{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

/* ===== Android App Section ===== */
function AndroidAppSection() {
  const appFeatures = [
    { title: "Offline-First", desc: "Works without connectivity, syncs when online" },
    { title: "QR Scanning", desc: "Camera-based barcode scanning at collection points" },
    { title: "Bluetooth Scale", desc: "Direct weight capture from connected scales" },
    { title: "GPS Geofencing", desc: "Location verification at HCF and CBWTF sites" },
    { title: "Multi-bag Sessions", desc: "Batch collection with session management" },
    { title: "Attendance Marking", desc: "Driver check-in at HCF locations" },
  ];

  return (
    <section className="section-spacing text-neutral-900">
      <div className="container-tight">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          {/* Content */}
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <p className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-3">
              Field Operations
            </p>
            <h2 className="mb-6 text-neutral-900">Powerful Android app for drivers & operators</h2>
            <p className="text-lg text-[#525252] mb-8 leading-relaxed">
              Purpose-built for field staff. Our Android app enables waste collection, verification, 
              and HCF registration—even in areas with poor connectivity. Every action is GPS-tagged 
              and Bluetooth-verified for complete auditability.
            </p>
            
            <div className="grid grid-cols-2 gap-4">
              {appFeatures.map((feature, i) => (
                <motion.div
                  key={feature.title}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.05 }}
                  className="flex items-start gap-3"
                >
                  <div className="w-5 h-5 rounded-full bg-[#047857]/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="font-semibold text-[#1a1a1a] text-sm">{feature.title}</h4>
                    <p className="text-xs text-[#525252]">{feature.desc}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>

          {/* Phone mockups */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="relative flex justify-center items-center"
          >
            <div className="relative">
              {/* Main phone */}
              <div className="relative z-10">
                <Image
                  src="/screenshots/app-2.png"
                  alt="SmartCBWTF Android App - Home Dashboard"
                  width={280}
                  height={560}
                  className="rounded-3xl shadow-2xl"
                />
              </div>
              
              {/* Secondary phone (behind, offset) */}
              <div className="absolute -right-16 top-12 -z-10 opacity-80">
                <Image
                  src="/screenshots/app-3.png"
                  alt="SmartCBWTF Android App - Scan & Weigh"
                  width={240}
                  height={480}
                  className="rounded-3xl shadow-xl"
                />
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}

/* ===== Portals Section ===== */
function PortalsSection() {
  const portals = [
    {
      title: "CBWTF Admin",
      description: "Full operational control—manage routes, HCFs, billing, staff, and compliance from one unified dashboard.",
      features: ["Route Management", "HCF Approvals", "Invoice Generation", "Staff Control"],
      color: "border-l-emerald-600",
    },
    {
      title: "HCF Admin",
      description: "Self-service waste tracking for healthcare facilities. Generate labels, view reports, and manage compliance.",
      features: ["Waste Logging", "Label Generation", "Report Access", "Consumable Orders"],
      color: "border-l-blue-600",
    },
    {
      title: "Management",
      description: "Executive oversight with approval workflows, financial dashboards, and compliance verification.",
      features: ["Approval Workflows", "Financial Oversight", "Audit Reports", "Multi-facility View"],
      color: "border-l-amber-600",
    },
  ];

  return (
    <section className="section-spacing bg-[#f5f0e8] text-neutral-900">
      <div className="container-tight">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
          {/* Left side - Portals list */}
          <div>
            <motion.p
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              className="text-sm font-semibold text-[#047857] uppercase tracking-wider mb-3"
            >
              Role-Based Access
            </motion.p>
            <motion.h2
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="mb-4 text-neutral-900"
            >
              Three portals, one platform
            </motion.h2>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 }}
              className="text-lg text-[#525252] mb-10"
            >
              Each stakeholder sees only what they need. Strict role separation ensures data security and operational clarity.
            </motion.p>

            <div className="space-y-6">
              {portals.map((portal, i) => (
                <motion.div
                  key={portal.title}
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                  className={`bg-white border-l-4 ${portal.color} rounded-r-xl p-6 shadow-sm`}
                >
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{portal.title}</h3>
                  <p className="text-sm text-[#525252] mb-4">{portal.description}</p>
                  <div className="flex flex-wrap gap-2">
                    {portal.features.map(f => (
                      <span key={f} className="text-xs px-2 py-1 bg-neutral-100 text-neutral-600 rounded">
                        {f}
                      </span>
                    ))}
                  </div>
                </motion.div>
              ))}
            </div>
          </div>

          {/* Right side - Dashboard mockup */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="flex justify-center"
          >
            <div className="screenshot-frame">
              <Image
                src="/screenshots/platform-3.png"
                alt="SmartCBWTF Admin Dashboard"
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
              <p className="text-4xl lg:text-5xl font-bold !text-[#34d399] mb-2">{stat.value}</p>
              <p className="text-sm !text-neutral-300 font-medium">{stat.label}</p>
            </motion.div>
          ))}
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

function GpsIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  );
}

function QrIcon() {
  return (
    <svg className="w-6 h-6 text-[#047857]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
    </svg>
  );
}

function ReportIcon() {
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
