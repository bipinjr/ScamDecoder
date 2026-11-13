// WhatsApp bot webhook (Twilio). In the Twilio console set "When a message comes in" to
// https://<your-domain>/api/whatsapp (POST).
import { answer, twilioSignatureOk, type Media } from "@/lib/bot";

export const runtime = "nodejs";
export const maxDuration = 30;

const xml = (s: string) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

export async function POST(req: Request) {
  const sid = process.env.TWILIO_ACCOUNT_SID, token = process.env.TWILIO_AUTH_TOKEN;
  if (!sid || !token) return new Response("not configured", { status: 503 });

  const params: Record<string, string> = {};
  for (const [k, v] of (await req.formData().catch(() => new FormData())).entries()) if (typeof v === "string") params[k] = v;

  // Twilio signs the exact public URL it called. Behind a proxy req.url can differ, so allow an override.
  const url = process.env.WHATSAPP_WEBHOOK_URL || req.url;
  if (!twilioSignatureOk(token, url, params, req.headers.get("x-twilio-signature") ?? ""))
    return new Response("forbidden", { status: 403 });

  let media: Media | null = null;
  const mediaUrl = params.MediaUrl0;
  if (Number(params.NumMedia) > 0 && mediaUrl) {
    try {
      // Only ever fetch from Twilio itself, and only then send our credentials.
      if (new URL(mediaUrl).hostname.endsWith(".twilio.com")) {
        const res = await fetch(mediaUrl, { headers: { Authorization: `Basic ${Buffer.from(`${sid}:${token}`).toString("base64")}` } });
        if (res.ok) media = { bytes: await res.arrayBuffer(), type: params.MediaContentType0 || res.headers.get("content-type") || "" };
      }
    } catch (e) {
      console.error("[whatsapp] download", (e as Error).message);
    }
  }

  const text = await answer(params.Body ?? "", media, "auto", "whatsapp");
  return new Response(`<?xml version="1.0" encoding="UTF-8"?><Response><Message>${xml(text)}</Message></Response>`, { headers: { "content-type": "text/xml" } });
}
