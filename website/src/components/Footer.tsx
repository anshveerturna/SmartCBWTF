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
              <Image
                src="/logo.svg"
                alt="SmartCBWTF"
                width={40}
                height={40}
                className="flex-shrink-0"
              />
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
