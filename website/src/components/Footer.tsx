import Link from "next/link";
import Image from "next/image";

const footerLinks = {
  Platform: [
    { href: "/platform", label: "Overview" },
    { href: "/features", label: "Features" },
    { href: "/security", label: "Security" },
    { href: "/compliance", label: "Compliance" },
  ],
  Company: [
    { href: "/about", label: "About Us" },
    { href: "/contact", label: "Contact" },
  ],
  Legal: [
    { href: "/privacy", label: "Privacy Policy" },
    { href: "/terms", label: "Terms of Service" },
  ],
  Portals: [
    { href: "/login", label: "CBWTF Admin" },
    { href: "/login", label: "HCF Admin" },
    { href: "/login", label: "Management" },
  ],
};

export function Footer() {
  return (
    <footer className="bg-neutral-900 text-white">
      <div className="container-tight py-16 lg:py-20">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-10 lg:gap-16">
          {/* Brand Column */}
          <div className="col-span-2 md:col-span-1">
            <Link href="/" className="inline-flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-full bg-emerald-600 flex items-center justify-center">
                <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <span className="font-bold text-lg text-white">SmartCBWTF</span>
            </Link>
            <p className="text-sm text-neutral-400 leading-relaxed max-w-xs">
              The operating system for bio-medical waste compliance. Built for regulatory excellence.
            </p>
          </div>

          {/* Link Columns */}
          {Object.entries(footerLinks).map(([category, links]) => (
            <div key={category}>
              <h4 className="text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-5">
                {category}
              </h4>
              <ul className="space-y-3">
                {links.map((link, i) => (
                  <li key={`${link.href}-${i}`}>
                    <Link
                      href={link.href}
                      className="text-sm text-neutral-300 hover:text-white transition-colors"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom Bar */}
        <div className="mt-16 pt-8 border-t border-neutral-700 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-sm text-neutral-400">
            © {new Date().getFullYear()} SmartCBWTF. All rights reserved.
          </p>
          <p className="text-xs text-neutral-500">
            Built for regulatory compliance. Designed for operational excellence.
          </p>
        </div>
      </div>
    </footer>
  );
}
