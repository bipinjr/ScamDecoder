// Pure bot helpers (no network): reply text and webhook authentication.
import { createHmac, timingSafeEqual } from "node:crypto";
import type { Analysis } from "./shared";

export const HELP =
  "DECODR\n\nForward me any suspicious message, screenshot or voice note. I will tell you if it is a scam, show the tricks it uses, and say what to do next.\n\nLost money already? Call 1930 or report at cybercrime.gov.in";

const STAMP = { scam: "🚨 SCAM", suspicious: "⚠️ BE CAREFUL", likely_safe: "✅ LOOKS GENUINE" } as const;

export function formatReply(a: Analysis, heard = ""): string {
  const lines = [`${STAMP[a.verdict]} · risk ${a.risk}/100`, a.summary];
  if (heard) lines.unshift(`I read: “${heard.slice(0, 160)}${heard.length > 160 ? "…" : ""}”\n`);
  if (a.whatTheyWant) lines.push(`They want: ${a.whatTheyWant}`);
  if (a.tactics.length)
    lines.push("", ...a.tactics.slice(0, 5).map((t, i) => `${i + 1}. ${t.name}${t.quote ? ` (“${t.quote.replace(/"/g, "").slice(0, 80)}”)` : ""}${t.explanation ? `: ${t.explanation}` : ""}`));
  if (a.safeActions.length) lines.push("", ...a.safeActions.slice(0, 4).map((s) => `• ${s}`));
  if (a.lesson) lines.push("", `Remember: ${a.lesson}`);
  return lines.join("\n").slice(0, 1500); // WhatsApp via Twilio caps a message at 1600 characters
}

// Twilio signs each webhook: base64(HMAC-SHA1(authToken, url + each param name+value, sorted by name)).
export function twilioSignatureOk(authToken: string, url: string, params: Record<string, string>, signature: string): boolean {
  const data = url + Object.keys(params).sort().map((k) => k + params[k]).join("");
  const want = createHmac("sha1", authToken).update(data).digest();
  const got = Buffer.from(signature, "base64");
  return got.length === want.length && timingSafeEqual(got, want);
}

export function secretOk(got: string | null, want: string | undefined): boolean {
  if (!want || !got) return false; // no secret configured = webhook stays closed
  const a = Buffer.from(got), b = Buffer.from(want);
  return a.length === b.length && timingSafeEqual(a, b);
}
