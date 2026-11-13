import Link from "next/link";
import { HookMark } from "./Logo";

// Landing hero: the scope. The DECODR hook inside scanning rings, with the tricks found in one example message.
export function Hero() {
  return (
    <section className="sd-hero">
      <div>
        <p className="claim-stamp">Scam detection for India, in 15 languages</p>
        <h1 className="mt-6">Decode the deception. <span>Stay one step ahead.</span></h1>
        <p className="mt-6 max-w-xl text-lg text-ink-soft">
          Check any suspicious message, voice note, screenshot or QR code. See whether it is a scam, which tricks it uses, and what to do next.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/check" className="btn-primary rounded-xl px-6 py-3.5 text-lg">Check a message</Link>
          <Link href="/radar" className="btn-ghost rounded-xl px-6 py-3.5 text-lg">See the Scam Radar</Link>
        </div>
        <ul className="mt-9 flex flex-wrap gap-x-7 gap-y-2 text-sm text-ink-soft">
          <li>Works on SMS, WhatsApp, Telegram and in your browser</li>
          <li>Scams you check appear on the live Scam Radar, with numbers removed</li>
        </ul>
      </div>

      <div className="sd-scope" aria-hidden>
        <div className="sd-sweep" />
        <svg viewBox="0 0 520 520">
          <circle className="ring dash" cx="260" cy="260" r="250" />
          <circle className="ring" cx="260" cy="260" r="205" />
          <circle className="ring arc" cx="260" cy="260" r="205" />
          <circle className="ring dash" cx="260" cy="260" r="160" />
          <circle className="ring arc rev" cx="260" cy="260" r="160" />
          <path className="trace" d="M330 215 H400 L430 150 H470" />
          <path className="trace" d="M338 275 H410 L440 300 H500" />
          <path className="trace" d="M190 300 H130 L100 380 H40" />
          <path className="trace" d="M185 220 H120 L95 150 H50" />
          <g className="hook" transform="translate(120 120) scale(1.4)"><HookMark tile={false} /></g>
        </svg>
        <span className="sd-tag" style={{ right: "0%", top: "22%" }}>Fake deadline</span>
        <span className="sd-tag" style={{ right: "-4%", top: "53%" }}>Spoofed bank link</span>
        <span className="sd-tag" style={{ left: "0%", top: "70%" }}>Asks for your OTP</span>
        <span className="sd-tag ok" style={{ left: "2%", top: "23%" }}>Real bank OTP: safe</span>
        <p className="sd-note">Examples of what a check points out.</p>
      </div>
    </section>
  );
}
