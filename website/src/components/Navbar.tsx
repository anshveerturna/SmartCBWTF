"use client";

import Link from "next/link";
import Image from "next/image";
import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

const navLinks = [
  { href: "/platform", label: "Platform" },
  { href: "/features", label: "Features" },
  { href: "/compliance", label: "Compliance" },
  { href: "/security", label: "Security" },
  { href: "/about", label: "About" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <motion.header
      initial={{ y: -100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
        scrolled
          ? "py-3 bg-white/80 backdrop-blur-xl shadow-sm border-b border-[#e7e5e4]"
          : "py-5 bg-transparent"
      }`}
    >
      <nav className="container-tight flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-3 group">
          <Image
            src="/logo.svg"
            alt="SmartCBWTF"
            width={40}
            height={40}
            className="flex-shrink-0 transition-transform group-hover:scale-105"
            priority
          />
          <span className="text-[#1a1a1a] font-bold text-xl tracking-tight hidden sm:block">
            SmartCBWTF
          </span>
        </Link>

        {/* Desktop Navigation */}
        <div className="hidden lg:flex items-center gap-8">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="text-[15px] font-medium text-[#525252] hover:text-[#1a1a1a] transition-colors relative group"
            >
              {link.label}
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-[#047857] transition-all group-hover:w-full" />
            </Link>
          ))}
        </div>

        {/* CTA Buttons */}
        <div className="hidden md:flex items-center gap-4">
          <Link
            href="/login"
            className="text-[15px] font-medium text-[#525252] hover:text-[#1a1a1a] transition-colors"
          >
            Log in
          </Link>
          <Link
            href="/contact"
            className="px-5 py-2.5 text-[15px] font-semibold text-white bg-[#047857] hover:bg-[#065f46] rounded-lg transition-all shadow-sm hover:shadow-md"
          >
            Request Demo
          </Link>
        </div>

        {/* Mobile Menu Button */}
        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="lg:hidden p-2 text-[#525252] hover:text-[#1a1a1a] transition-colors"
          aria-label="Toggle menu"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {mobileOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
      </nav>

      {/* Mobile Menu */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.3 }}
            className="lg:hidden bg-white border-t border-[#e7e5e4]"
          >
            <div className="container-tight py-6 flex flex-col gap-2">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileOpen(false)}
                  className="px-4 py-3 text-[#525252] hover:text-[#1a1a1a] hover:bg-[#f5f0e8] rounded-lg transition-colors font-medium"
                >
                  {link.label}
                </Link>
              ))}
              <div className="flex flex-col gap-3 mt-4 pt-4 border-t border-[#e7e5e4]">
                <Link
                  href="/login"
                  onClick={() => setMobileOpen(false)}
                  className="px-4 py-3 text-center text-[#1a1a1a] hover:bg-[#f5f0e8] rounded-lg transition-colors font-medium"
                >
                  Log in
                </Link>
                <Link
                  href="/contact"
                  onClick={() => setMobileOpen(false)}
                  className="px-4 py-3 text-center text-white bg-[#047857] hover:bg-[#065f46] rounded-lg transition-colors font-semibold"
                >
                  Request Demo
                </Link>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.header>
  );
}
