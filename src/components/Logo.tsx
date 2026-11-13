// DECODR hook mark, from the official brand file. Scrambled line = the bait, solid hook = decoded, scan line = the moment of truth.
export function HookMark({ tile = true }: { tile?: boolean }) {
  return (
    <>
      <defs>
        <linearGradient id="decodr-g" gradientUnits="userSpaceOnUse" x1="50" y1="100" x2="118" y2="154">
          <stop offset="0" stopColor="#22E6C3" /><stop offset="1" stopColor="#7B6CFF" />
        </linearGradient>
      </defs>
      {tile && <rect width="200" height="200" rx="46" fill="#0E1426" />}
      <circle cx="118" cy="30" r="7" fill="none" stroke="#6D7AA0" strokeWidth="5" />
      <path d="M118 40V104" fill="none" stroke="#6D7AA0" strokeWidth="12" strokeDasharray="7 5 15 5 4 5 9 5" />
      <path d="M118 104V120A34 34 0 0 1 50 120V94" fill="none" stroke="url(#decodr-g)" strokeWidth="12" strokeLinecap="round" />
      <path d="M38 96L50 68L62 96Z" fill="url(#decodr-g)" />
      <rect x="26" y="98" width="148" height="12" fill="#22E6C3" opacity="0.14" />
      <rect x="26" y="103" width="148" height="2" rx="1" fill="#22E6C3" />
      <rect x="140" y="46" width="6" height="6" fill="#6D7AA0" />
      <rect x="152" y="60" width="6" height="6" fill="#6D7AA0" opacity="0.6" />
      <rect x="144" y="76" width="6" height="6" fill="#6D7AA0" opacity="0.4" />
      <rect x="158" y="42" width="5" height="5" fill="#6D7AA0" opacity="0.5" />
      <rect x="138" y="88" width="5" height="5" fill="#6D7AA0" opacity="0.7" />
      <rect x="160" y="84" width="5" height="5" fill="#6D7AA0" opacity="0.35" />
    </>
  );
}

export function LogoMark({ size = 32, className = "" }: { size?: number; className?: string }) {
  return (
    <svg viewBox="0 0 200 200" width={size} height={size} className={className} aria-hidden>
      <HookMark />
    </svg>
  );
}

export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2.5 ${className}`}>
      <LogoMark size={34} />
      <span className="text-[1.3rem] font-bold leading-none tracking-[0.08em] text-[#F2F5FF]" style={{ fontFamily: "var(--font-display), sans-serif" }}>DECODR</span>
    </span>
  );
}
