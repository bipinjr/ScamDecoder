import { extractHosts } from "./signals";
import { redact } from "./redact";
export { redact };
import { PRINCIPLE_IDS, type PrincipleId } from "./principles";
import type { Analysis } from "./shared";

// Tolerate copy-paste slips in hosting env settings: whitespace, wrapping quotes, trailing slash.
const clean = (v = "") => v.trim().replace(/^["']|["']$/g, "").trim();
const URL = ((u) => (u && !/^https?:\/\//.test(u) ? `https://${u}` : u))(clean(process.env.SUPABASE_URL).replace(/\/+$/, ""));
const KEY = clean(process.env.SUPABASE_KEY);
const headers = { apikey: KEY, Authorization: `Bearer ${KEY}`, "content-type": "application/json" };
const DB_SET = /^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(URL) && KEY.length > 20;

export const CHANNELS = ["sms", "whatsapp", "telegram", "email", "call", "other"] as const;
export type Channel = (typeof CHANNELS)[number];

export type ReportInput = {
  text: string;
  scamType: string;
  verdict: "scam" | "suspicious" | "likely_safe";
  risk: number;
  principles: string[];
  channel: string;
  language: string;
  state?: string;
};

export type RadarSummary = {
  total: number; scams: number; avg_risk: number; seeded: number;
  by_type: { label: string; n: number }[];
  by_principle: { id: PrincipleId; n: number }[];
  by_channel: { label: Channel; n: number }[];
  by_day: { day: string; n: number }[];
  top_domains: { label: string; n: number }[];
};
export type RadarReport = {
  id: string; created_at: string; scam_type: string; verdict: string; risk: number;
  channel: Channel; language: string; state: string | null; excerpt: string; principles: PrincipleId[]; is_seed: boolean;
  domains?: string[];
};
export type Radar = { summary: RadarSummary; recent: RadarReport[]; source: "database" | "local" };

function toRow(r: ReportInput) {
  return {
    scam_type: r.scamType.slice(0, 80).padEnd(2, "?") || "Unknown",
    verdict: r.verdict,
    risk: Math.max(0, Math.min(100, Math.round(r.risk))),
    principles: [...new Set(r.principles)].filter((p): p is PrincipleId => PRINCIPLE_IDS.includes(p as PrincipleId)),
    channel: ((CHANNELS as readonly string[]).includes(r.channel) ? r.channel : "other") as Channel,
    language: r.language.slice(0, 20),
    state: r.state ? r.state.slice(0, 40) : null,
    excerpt: redact(r.text).padEnd(5, "."),
    domains: extractHosts(r.text).slice(0, 5),
  };
}

// shortcut: used when Supabase is not set up or unreachable. Lives in this server's memory only (lost on restart,
// not shared between serverless instances); fine for a local demo, upgrade = run supabase/schema.sql.
const g = globalThis as unknown as { __decodrRadar?: RadarReport[] };
const local = (g.__decodrRadar ??= []);

export async function saveReport(r: ReportInput): Promise<"database" | "local"> {
  const row = toRow(r);
  if (DB_SET) {
    try {
      const res = await fetch(`${URL}/rest/v1/reports`, { method: "POST", headers: { ...headers, Prefer: "return=minimal" }, body: JSON.stringify(row), cache: "no-store" });
      if (res.ok) return "database";
      console.error("[radar] save failed:", res.status, (await res.text()).slice(0, 160));
    } catch (e) {
      console.error("[radar] save failed:", (e as Error).message);
    }
  }
  local.unshift({ ...row, id: crypto.randomUUID(), created_at: new Date().toISOString(), is_seed: false });
  local.splice(500);
  return "local";
}

// Every scam or suspicious verdict, from any door, lands on the Radar. Genuine messages are never stored.
export async function recordCheck(text: string, a: Analysis, channel: string, language: string): Promise<boolean> {
  if (a.verdict === "likely_safe") return false;
  try {
    await saveReport({ text, scamType: a.scamType || "Suspicious message", verdict: a.verdict, risk: a.risk,
      principles: a.tactics.map((t) => t.principle ?? ""), channel, language });
    return true;
  } catch (e) {
    console.error("[radar] record failed:", (e as Error).message);
    return false;
  }
}

function summarise(rows: RadarReport[]): RadarSummary {
  const since = Date.now() - 14 * 86400000;
  const r = rows.filter((x) => new Date(x.created_at).getTime() > since);
  const count = <T,>(items: T[]) => { const m = new Map<T, number>(); items.forEach((i) => m.set(i, (m.get(i) ?? 0) + 1)); return [...m].sort((a, b) => b[1] - a[1]); };
  return {
    total: r.length,
    scams: r.filter((x) => x.verdict === "scam").length,
    avg_risk: r.length ? Math.round(r.reduce((s, x) => s + x.risk, 0) / r.length) : 0,
    seeded: 0,
    by_type: count(r.filter((x) => x.verdict !== "likely_safe").map((x) => x.scam_type)).slice(0, 6).map(([label, n]) => ({ label, n })),
    by_principle: count(r.flatMap((x) => x.principles)).map(([id, n]) => ({ id, n })),
    by_channel: count(r.map((x) => x.channel)).map(([label, n]) => ({ label, n })),
    by_day: count(r.map((x) => x.created_at.slice(0, 10))).map(([day, n]) => ({ day, n })),
    top_domains: count(r.flatMap((x) => x.domains ?? [])).slice(0, 5).map(([label, n]) => ({ label, n })),
  };
}

export async function getRadar(): Promise<Radar> {
  if (DB_SET) {
    try {
      const [s, r] = await Promise.all([
        fetch(`${URL}/rest/v1/rpc/radar_summary`, { method: "POST", headers, body: JSON.stringify({ days: 14 }), cache: "no-store" }),
        fetch(`${URL}/rest/v1/reports?select=id,created_at,scam_type,verdict,risk,channel,language,state,excerpt,principles,is_seed&verdict=neq.likely_safe&order=created_at.desc&limit=12`, { headers, cache: "no-store" }),
      ]);
      if (s.ok && r.ok) return { summary: await s.json(), recent: await r.json(), source: "database" };
      console.error("[radar] read failed:", s.status, r.status);
    } catch (e) {
      console.error("[radar] read failed:", (e as Error).message);
    }
  }
  return { summary: summarise(local), recent: local.slice(0, 12), source: "local" };
}
