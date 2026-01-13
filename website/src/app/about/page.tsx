"use client";

import { motion } from "framer-motion";
import Image from "next/image";
import Link from "next/link";

export default function AboutPage() {
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
              About Us
            </motion.p>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-6"
            >
              Making compliance effortless
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252] leading-relaxed"
            >
              SmartCBWTF is building the infrastructure for regulatory-compliant biomedical waste management. We believe that compliance shouldn't be a burden—it should be built into every workflow.
            </motion.p>
          </div>
        </div>
      </section>

      {/* Mission Section */}
      <section className="section-spacing bg-[#f5f0e8]">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="mb-6">Our mission</h2>
              <p className="text-lg text-[#525252] leading-relaxed mb-6">
                We started SmartCBWTF because we saw a critical gap in the biomedical waste industry. CBWTFs were struggling with manual processes, revenue leakage, and the constant pressure of regulatory compliance.
              </p>
              <p className="text-lg text-[#525252] leading-relaxed mb-6">
                Our mission is simple: <strong className="text-[#1a1a1a]">make regulatory compliance automatic</strong>. By digitizing every touchpoint—from waste generation to final treatment—we create an immutable chain of custody that satisfies regulators and protects operators.
              </p>
              <p className="text-lg text-[#525252] leading-relaxed">
                We're building the operating system that CBWTFs and healthcare facilities deserve: modern, reliable, and built for compliance from day one.
              </p>
            </motion.div>
            
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="relative"
            >
              <div className="screenshot-frame">
                <Image
                  src="/screenshots/platform-1.png"
                  alt="SmartCBWTF Dashboard"
                  width={600}
                  height={400}
                  className="rounded-lg"
                />
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Values */}
      <section className="section-spacing">
        <div className="container-tight">
          <div className="max-w-3xl mx-auto text-center mb-16">
            <h2 className="mb-4">What we believe</h2>
            <p className="text-lg text-[#525252]">
              The principles that guide how we build SmartCBWTF
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                title: "Compliance First",
                desc: "Every feature starts with regulatory requirements. We don't retrofit compliance—we build it in.",
              },
              {
                title: "Operational Excellence",
                desc: "Software should make your work easier, not harder. We obsess over usability and workflow efficiency.",
              },
              {
                title: "Data Integrity",
                desc: "Immutable records, complete audit trails. The truth should be provable and tamper-proof.",
              },
              {
                title: "Stakeholder Empowerment",
                desc: "CBWTFs, HCFs, and regulators all benefit. We design for everyone in the ecosystem.",
              },
              {
                title: "Continuous Improvement",
                desc: "We listen to users and iterate constantly. The platform evolves with the industry.",
              },
              {
                title: "Transparency",
                desc: "Clear pricing, honest communication, and complete visibility into platform operations.",
              },
            ].map((value, i) => (
              <motion.div
                key={value.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="border-l-4 border-[#047857] pl-6"
              >
                <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">{value.title}</h3>
                <p className="text-[#525252]">{value.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 bg-[#1a1a1a] text-white">
        <div className="container-tight text-center">
          <h2 className="text-white mb-6">Join us in modernizing waste compliance</h2>
          <p className="text-neutral-400 mb-8 max-w-xl mx-auto">
            Whether you're a CBWTF operator or a healthcare facility, we'd love to show you what's possible.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-4">
            <Link
              href="/contact"
              className="px-8 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all"
            >
              Get in Touch
            </Link>
            <Link
              href="/platform"
              className="px-8 py-4 text-white font-semibold border border-neutral-700 rounded-lg transition-all hover:bg-neutral-800"
            >
              Explore Platform
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
