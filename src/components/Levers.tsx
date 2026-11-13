import { CITATIONS, PRINCIPLES, PRINCIPLE_IDS } from "@/lib/principles";

// The seven levers every scam pulls (Stajano & Wilson, 2011). Each trick DECODR finds is tagged with one of these.
export function Levers() {
  return (
    <section id="levers" className="mx-auto mt-28 max-w-7xl scroll-mt-24 px-4 sm:px-6">
      <div className="max-w-3xl">
        <h2>How DECODR spots a scam: the 7 levers</h2>
        <p className="mt-4 text-lg text-ink-soft">
          A scam does not hack your phone. It pulls a lever in your head. Every trick DECODR finds in a message is tagged with one of
          these seven, from Cambridge research into how scam victims are fooled.
        </p>
      </div>
      <ul className="mt-12 grid gap-px overflow-hidden rounded-2xl border hairline bg-[rgb(123_108_255/.14)] sm:grid-cols-2 lg:grid-cols-4">
        {PRINCIPLE_IDS.map((id) => {
          const p = PRINCIPLES[id];
          return (
            <li key={id} id={`lever-${id}`} className="flex flex-col bg-[#0a0f1e] p-6">
              <h3 className="text-white">{p.name}</h3>
              <p className="mt-2 text-ink-soft">{p.long}</p>
              <p className="mt-auto pt-5 font-mono text-sm text-[#ff8a96]">“{p.example}”</p>
            </li>
          );
        })}
        <li className="flex flex-col justify-center bg-[#0a0f1e] p-6 text-sm text-ink-soft">
          <p>Source</p>
          <a className="mt-1 font-semibold text-white underline underline-offset-4" href={CITATIONS.stajano.url} target="_blank" rel="noreferrer">
            {CITATIONS.stajano.label}: {CITATIONS.stajano.title}
          </a>
          <a className="mt-3 underline underline-offset-4 hover:text-white" href={CITATIONS.stajano.pdf} target="_blank" rel="noreferrer">Read the paper (PDF)</a>
        </li>
      </ul>
    </section>
  );
}
