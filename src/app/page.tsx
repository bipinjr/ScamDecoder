import Link from "next/link";
import { CountUp } from "@/components/fx";
import { Hero } from "@/components/Hero";
import { WordReveal } from "@/components/WordReveal";
import { Dissection } from "@/components/Dissection";
import { Levers } from "@/components/Levers";
import { Ticker } from "@/components/Ticker";
import { CITATIONS } from "@/lib/principles";

const STEPS = [
  { title: "Rule engine finds hard evidence", body: "Fake bank links, hidden short links, lookalike domains, OTP or PIN requests, threats and deadlines in English and Hindi. It runs instantly in your browser, while you type." },
  { title: "AI reads it like a fraud investigator", body: "A large language model quotes the exact words doing the damage and tags each trick with the psychological principle it exploits. Messages that try to talk to the AI get flagged as a trick too." },
  { title: "You get an answer you understand", body: "A clear verdict and score, the message marked up in red pen, what the sender wants, and what to do now: call 1930, report it, or warn your family on WhatsApp." },
];

export default function Home() {
  return (
    <main>
      <Hero />

      <Ticker />

      {/* Numbers */}
      <section className="mx-auto max-w-7xl px-4 py-24 sm:px-6">
        <WordReveal
          className="max-w-4xl font-display text-4xl leading-tight text-white sm:text-6xl"
          text="Indians lost ₹22,845 crore to cyber fraud in a single year. Most of it started with one message."
          hl={["₹22,845", "crore", "one", "message"]}
        />
        <div className="mt-14 grid gap-px overflow-hidden rounded-3xl border hairline bg-white/[0.07] sm:grid-cols-2 lg:grid-cols-4">
          {[
            { n: <CountUp to={36} suffix=" lakh" />, l: "fraud cases reported in 2024" },
            { n: <CountUp to={206} suffix="%" />, l: "jump in money lost vs 2023" },
            { n: <CountUp to={7} />, l: "psychological levers every scam pulls" },
            { n: <CountUp to={15} />, l: "Indian languages supported" },
          ].map((s, i) => (
            <div key={i} className="reveal bg-paper p-7">
              <p className="text-4xl font-semibold tabular-nums tracking-tight text-white">{s.n}</p>
              <p className="mt-2 text-ink-soft">{s.l}</p>
            </div>
          ))}
        </div>
        <p className="mt-4 text-sm text-ink-soft">
          Source: <a className="underline underline-offset-4 hover:text-white" href={CITATIONS.mha.url} target="_blank" rel="noreferrer">{CITATIONS.mha.label}</a>
        </p>
      </section>

      <Dissection />

      <Levers />

      {/* How it works */}
      <section className="mx-auto mt-28 grid max-w-7xl gap-14 px-4 sm:px-6 lg:grid-cols-[minmax(0,24rem)_1fr]">
        <div className="reveal lg:sticky lg:top-28 lg:self-start">
          <h2 className="text-5xl sm:text-6xl">How it reads <em>a message</em></h2>
          <p className="mt-4 text-lg text-ink-soft">Two independent checks, one plain answer. If the AI is ever unavailable, the rule engine still answers.</p>
        </div>
        <ol className="steps-track relative space-y-16 pl-16">
          <svg className="absolute left-[1.15rem] top-2 h-[calc(100%-1rem)] w-6 overflow-visible" viewBox="0 0 24 100" preserveAspectRatio="none" aria-hidden>
            <defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#5b49e6" /><stop offset="1" stopColor="#7b6cff" /></linearGradient></defs>
            <path className="pen-path" pathLength={1} d="M12 0 C 2 12, 22 22, 12 34 S 2 56, 12 67 S 22 88, 12 100" fill="none" stroke="url(#g)" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
          {STEPS.map((s, i) => (
            <li key={s.title} className="reveal relative">
              <span className="absolute -left-16 -top-1 grid h-10 w-10 place-items-center rounded-full border-2 border-teal bg-paper font-pen text-2xl font-bold text-teal">{i + 1}</span>
              <h3 className="text-3xl text-white">{s.title}</h3>
              <p className="mt-2 max-w-2xl text-lg text-ink-soft">{s.body}</p>
            </li>
          ))}
        </ol>
      </section>

    </main>
  );
}
