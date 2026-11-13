import Link from "next/link";
import { LogoMark } from "./Logo";

export function Footer() {
  return (
    <footer className="relative mt-24 border-t hairline bg-night">
      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-14 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr]">
        <div>
          <span className="inline-flex items-center gap-2.5">
            <LogoMark size={34} />
            <span className="text-xl font-semibold text-white">DECODR</span>
          </span>
          <p className="mt-4 max-w-sm text-ink-soft">
            See through the scam. Read the hook. Research-backed scam protection for India. Guidance, not a guarantee. When a message looks like a scam, an anonymised copy goes on
            the Scam Radar to warn others. Genuine messages are never stored.
          </p>
        </div>
        <div>
          <p className="font-semibold text-white">Use it</p>
          <ul className="mt-3 space-y-2 text-ink-soft">
            <li><Link className="inline-block py-2 transition-colors hover:text-white" href="/check">Check a message</Link></li>
            <li><Link className="inline-block py-2 transition-colors hover:text-white" href="/radar">Scam Radar</Link></li>
            <li><Link className="inline-block py-2 transition-colors hover:text-white" href="/extension">Browser extension</Link></li>
          </ul>
        </div>
        <div>
          <p className="font-semibold text-white">Lost money to a scam?</p>
          <p className="mt-3 text-ink-soft">Call the helpline right away, or report it online.</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <a className="btn-primary inline-flex items-center rounded-xl px-5 py-3 font-semibold" href="tel:1930">Call 1930</a>
            <a className="btn-ghost inline-flex items-center rounded-xl px-5 py-3 font-semibold" href="https://cybercrime.gov.in" target="_blank" rel="noreferrer">cybercrime.gov.in</a>
          </div>
          <p className="mt-6 text-sm text-ink-soft/70">Built for Colossus 3.0 by Team Zenith: Bipin A, Darshan HS, Manoj Bharadwaj and Kaushal Ved</p>
        </div>
      </div>
    </footer>
  );
}
