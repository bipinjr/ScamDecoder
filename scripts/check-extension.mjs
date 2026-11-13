// Loads the real extension into Chromium and opens fake scam links, checking each one is stopped on the warning page.
// Run: node scripts/check-extension.mjs [path-to-chrome-or-edge]
// Needs Node 22+ (global WebSocket).
import { spawn } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";

const CHROMIUM = process.argv[2] || "C:/Program Files/Google/Chrome/Application/chrome.exe";
const EXT = resolve("extension");
const PORT = 9333;
// .example is reserved (RFC 2606): these can never resolve to a real site, even if the guard failed.
const CASES = [
  ["http://sbi-kyc-update.example/verify", true],
  ["http://paypa1-secure-login.example/account", true],
  ["http://hdfc-netbanking.example/login", true],
  ["https://www.sbi.co.in/", false],
];

const profile = mkdtempSync(join(tmpdir(), "decodr-ext-"));
const chrome = spawn(CHROMIUM, [
  "--headless=new", `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
  // Branded Chrome and Edge ignore --load-extension unless this feature is turned off.
  "--disable-features=DisableLoadExtensionCommandLineSwitch",
  `--disable-extensions-except=${EXT}`, `--load-extension=${EXT}`, "--no-first-run", "about:blank",
], { stdio: "ignore" });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const api = async (path, method = "GET") => (await fetch(`http://127.0.0.1:${PORT}${path}`, { method })).json();

let failed = 0;
try {
  await sleep(5000); // let the extension's service worker start
  const targets = await api("/json/list");
  const sw = targets.find((t) => t.type === "service_worker" && t.url.startsWith("chrome-extension://"));
  if (!sw) throw new Error("extension did not load");
  console.log("extension loaded:", sw.url.split("/")[2]);

  for (const [url, shouldBlock] of CASES) {
    const tab = await api(`/json/new?${encodeURIComponent(url)}`, "PUT");
    await sleep(4000);
    const now = (await api("/json/list")).find((t) => t.id === tab.id)?.url || "";
    const blocked = now.includes("/warning.html");
    const ok = blocked === shouldBlock;
    if (!ok) failed++;
    console.log(`${ok ? "PASS" : "FAIL"}  ${shouldBlock ? "should block" : "should allow"}  ${url}  ->  ${blocked ? "warning page" : now.slice(0, 60)}`);
    if (blocked) {
      const why = JSON.parse(new URL(now).searchParams.get("why") || "[]").map((r) => r.label);
      console.log("      reasons:", why.join(" | "));
    }
    await fetch(`http://127.0.0.1:${PORT}/json/close/${tab.id}`); // replies with plain text
  }
} catch (e) {
  failed++;
  console.error("FAIL", e.message);
} finally {
  chrome.kill();
  await sleep(500);
  try { rmSync(profile, { recursive: true, force: true }); } catch {}
}
console.log(failed ? `${failed} failed` : "all extension checks passed");
process.exit(failed ? 1 : 0);
