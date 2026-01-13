import Image from "next/image";
import Link from "next/link";

export function Logo({ size = 32, showText = true }: { size?: number; showText?: boolean }) {
  return (
    <Link href="/" className="flex items-center gap-2.5 group">
      <Image
        src="/logo.svg"
        alt="SmartCBWTF Logo"
        width={size}
        height={size}
        className="flex-shrink-0"
        priority
      />
      {showText && (
        <span className="text-[#0f172a] font-semibold text-lg tracking-tight group-hover:text-[#047857] transition-colors">
          SmartCBWTF
        </span>
      )}
    </Link>
  );
}
