"use client";

import { motion } from "framer-motion";
import { useState } from "react";

export default function ContactPage() {
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
  };

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
              Contact
            </motion.p>
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-balance mb-6"
            >
              Let's talk
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="text-xl text-[#525252] leading-relaxed"
            >
              Whether you're ready for a demo or have questions about how SmartCBWTF can help your facility, we're here to help.
            </motion.p>
          </div>
        </div>
      </section>

      {/* Contact Form & Info */}
      <section className="pb-24">
        <div className="container-tight">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16">
            {/* Form */}
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 }}
            >
              {submitted ? (
                <div className="bg-[#047857]/10 rounded-2xl p-12 text-center">
                  <div className="w-16 h-16 rounded-full bg-[#047857] flex items-center justify-center mx-auto mb-6">
                    <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-bold text-[#1a1a1a] mb-3">Message sent!</h3>
                  <p className="text-[#525252]">
                    Thank you for reaching out. We'll get back to you within 24 hours.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="bg-white rounded-2xl p-8 shadow-sm">
                  <h3 className="text-xl font-semibold text-[#1a1a1a] mb-6">Send us a message</h3>
                  
                  <div className="space-y-5">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      <div>
                        <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Name
                        </label>
                        <input
                          type="text"
                          required
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="Your name"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                          Organization
                        </label>
                        <input
                          type="text"
                          className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                          placeholder="Facility name"
                        />
                      </div>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                        Email
                      </label>
                      <input
                        type="email"
                        required
                        className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                        placeholder="you@example.com"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                        Phone
                      </label>
                      <input
                        type="tel"
                        className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all"
                        placeholder="+91 98765 43210"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                        I am a...
                      </label>
                      <select className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all">
                        <option value="">Select your role</option>
                        <option value="cbwtf">CBWTF Operator</option>
                        <option value="hcf">Healthcare Facility Admin</option>
                        <option value="regulator">Regulatory Body</option>
                        <option value="other">Other</option>
                      </select>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-[#1a1a1a] mb-2">
                        How can we help?
                      </label>
                      <textarea
                        rows={4}
                        required
                        className="w-full px-4 py-3 bg-[#f5f0e8] border-0 rounded-lg focus:ring-2 focus:ring-[#047857] transition-all resize-none"
                        placeholder="Tell us about your needs..."
                      />
                    </div>
                    
                    <button
                      type="submit"
                      className="w-full px-6 py-4 bg-[#047857] hover:bg-[#065f46] text-white font-semibold rounded-lg transition-all shadow-lg shadow-[#047857]/25"
                    >
                      Send Message
                    </button>
                  </div>
                </form>
              )}
            </motion.div>

            {/* Contact Info */}
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 }}
              className="lg:pt-12"
            >
              <div className="space-y-8">
                <div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">Email</h3>
                  <a href="mailto:contact@smartcbwtf.com" className="text-[#047857] hover:underline">
                    contact@smartcbwtf.com
                  </a>
                </div>
                
                <div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">Support</h3>
                  <a href="mailto:support@smartcbwtf.com" className="text-[#047857] hover:underline">
                    support@smartcbwtf.com
                  </a>
                </div>
                
                <div>
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-2">Response Time</h3>
                  <p className="text-[#525252]">
                    We typically respond within 24 hours during business days.
                  </p>
                </div>
                
                <div className="pt-8 border-t border-[#e7e5e4]">
                  <h3 className="text-lg font-semibold text-[#1a1a1a] mb-4">Prefer a call?</h3>
                  <p className="text-[#525252] mb-4">
                    Schedule a time that works for you and we'll call you for a personalized demo.
                  </p>
                  <button className="px-6 py-3 bg-[#f5f0e8] text-[#1a1a1a] font-semibold rounded-lg hover:bg-[#e7e5e4] transition-all">
                    Schedule a Call
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>
    </div>
  );
}
