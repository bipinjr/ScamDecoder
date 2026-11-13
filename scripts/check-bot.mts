// Self-check for the bot helpers. Run: node --experimental-strip-types scripts/check-bot.mts
import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { formatReply, twilioSignatureOk, secretOk } from "../src/lib/bot-core.ts";

// Twilio signature: accept the real one, reject a changed body, a changed URL and junk.
const token = "test-token", url = "https://example.test/api/whatsapp";
const params = { Body: "hello", From: "whatsapp:+10000000000", NumMedia: "0" };
const sign = (u: string, p: Record<string, string>) => createHmac("sha1", token).update(u + Object.keys(p).sort().map((k) => k + p[k]).join("")).digest("base64");
const good = sign(url, params);
assert.ok(twilioSignatureOk(token, url, params, good));
assert.ok(!twilioSignatureOk(token, url, { ...params, Body: "changed" }, good), "tampered body accepted");
assert.ok(!twilioSignatureOk(token, url + "?x=1", params, good), "different URL accepted");
assert.ok(!twilioSignatureOk(token, url, params, ""), "empty signature accepted");
assert.ok(!twilioSignatureOk(token, url, params, "not-base64!!"), "junk signature accepted");

// Telegram secret: closed when unset, exact match only.
assert.ok(!secretOk("anything", undefined), "open webhook when no secret is configured");
assert.ok(!secretOk(null, "s3cret"));
assert.ok(!secretOk("s3cres", "s3cret"));
assert.ok(secretOk("s3cret", "s3cret"));

// Reply text: verdict first, capped for WhatsApp.
const reply = formatReply({
  verdict: "scam", risk: 94, scamType: "Fake KYC", summary: "This is a fake bank message.", whatTheyWant: "Your OTP",
  tactics: [{ name: "Fake deadline", quote: "BLOCKED today", explanation: "Rushes you." }],
  safeActions: ["Do not click the link.", "Call 1930."], lesson: "Banks never ask for an OTP.".repeat(100),
  signals: [], ruleScore: 80, aiScore: 94, model: "x",
});
assert.ok(reply.startsWith("🚨 SCAM · risk 94/100"));
assert.ok(reply.includes("1. Fake deadline") && reply.includes("• Call 1930."));
assert.ok(reply.length <= 1500);
console.log("bot checks passed");
