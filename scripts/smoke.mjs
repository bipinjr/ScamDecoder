// Pre-demo smoke test: every DECODR door on the deployed site, in one run.
// Run: node scripts/smoke.mjs            (defaults to the live site)
//      node scripts/smoke.mjs http://localhost:3000
import { deflateSync } from "node:zlib";

const BASE = (process.argv[2] || "https://scamdecoderjr.vercel.app").replace(/\/$/, "");
let failed = 0;
const ok = (name, pass, info = "") => { if (!pass) failed++; console.log(`${pass ? "PASS" : "FAIL"}  ${name}${info ? "  (" + info + ")" : ""}`); };
const time = async (f) => { const t = Date.now(); const r = await f(); return [r, ((Date.now() - t) / 1000).toFixed(1) + "s"]; };
const post = (path, body, type = "application/json") =>
  fetch(BASE + path, { method: "POST", headers: { "content-type": type }, body: type === "application/json" ? JSON.stringify(body) : body, signal: AbortSignal.timeout(45000) });

// A tiny white PNG, so the image reader is exercised without a file on disk.
function png(w = 64, h = 32) {
  const crc = (b) => { let c, t = []; for (let n = 0; n < 256; n++) { c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } let x = 0xffffffff; for (const v of b) x = t[(x ^ v) & 255] ^ (x >>> 8); return (x ^ 0xffffffff) >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 2;
  const raw = Buffer.alloc((w * 3 + 1) * h, 255); for (let y = 0; y < h; y++) raw[y * (w * 3 + 1)] = 0;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk("IHDR", ihdr), chunk("IDAT", deflateSync(raw)), chunk("IEND", Buffer.alloc(0))]);
}

console.log(`DECODR smoke test against ${BASE}\n`);

for (const p of ["/", "/check", "/radar", "/extension", "/share", "/downloads/decodr.apk", "/downloads/scamdecoder-chrome.zip", "/downloads/scamdecoder-firefox.zip", "/manifest.webmanifest"]) {
  const r = await fetch(BASE + p, { method: p.includes("downloads") ? "HEAD" : "GET" }).catch(() => null);
  ok(`page ${p}`, r?.status === 200, r ? String(r.status) : "no response");
}

const cases = [
  ["scam (English)", "Dear customer your SBI account will be blocked today. Update PAN at http://sbi-kyc-update.xyz now", (a) => a.verdict === "scam"],
  ["scam (Hindi)", "आपका बिजली कनेक्शन आज रात 9:30 बजे काट दिया जाएगा। तुरंत अधिकारी को 9876543210 पर कॉल करें", (a) => a.verdict !== "likely_safe"],
  ["safe bank OTP", "123456 is your OTP for login to HDFC Bank NetBanking. Do not share it with anyone. -HDFC Bank", (a) => a.verdict !== "scam"],
];
for (const [name, text, expect] of cases) {
  const [r, t] = await time(() => post("/api/analyze", { text, language: "English", channel: "web" }).then((x) => x.json()).catch((e) => ({ error: e.message })));
  ok(`analyze: ${name}`, !r.error && expect(r), `${r.verdict ?? r.error} ${r.risk ?? ""} in ${t}${r.model ? ", " + r.model : ""}`);
}

const [img, ti] = await time(() => post("/api/read-image", png(), "image/png").then(async (x) => ({ status: x.status, body: await x.json().catch(() => ({})) })).catch((e) => ({ status: 0, body: { error: e.message } })));
ok("read-image answers", img.status === 200 || img.status === 422, `${img.status} ${JSON.stringify(img.body).slice(0, 60)} in ${ti}`);

const radar = await fetch(BASE + "/api/radar").then((x) => x.json()).catch(() => null);
ok("radar feed", Array.isArray(radar?.recent), radar ? `${radar.recent.length} recent, source ${radar.source}` : "no data");
ok("radar on the database", radar?.source === "database", "local means Supabase is unreachable: the radar resets on each deploy");

const tg = await post("/api/telegram", { update_id: 1 }).catch(() => null);
ok("telegram webhook rejects unsigned calls", tg?.status === 403, tg ? String(tg.status) : "no response");

console.log(`\n${failed ? failed + " check(s) FAILED" : "All checks passed"}`);
process.exit(failed ? 1 : 0);
