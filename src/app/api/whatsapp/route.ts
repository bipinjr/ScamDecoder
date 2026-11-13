// WhatsApp bot webhook (Twilio). Set the incoming-message webhook to https://<your-domain>/api/whatsapp (POST):
// trial accounts: Try out WhatsApp > Receive a message > Auto-reply: Custom; paid accounts: Sandbox settings.
import { answer, twilioSignatureOk, type Media } from "@/lib/bot";

export const runtime = "nodejs";
export const maxDuration = 30;

export async function POST(req: Request) {
  const sid = process.env.TWILIO_ACCOUNT_SID, token = process.env.TWILIO_AUTH_TOKEN;
  if (!sid || !token) return new Response("not configured", { status: 503 });
  const auth = `Basic ${Buffer.from(`${sid}:${token}`).toString("base64")}`;

  const params: Record<string, string> = {};
  for (const [k, v] of (await req.formData().catch(() => new FormData())).entries()) if (typeof v === "string") params[k] = v;

  // Twilio signs the exact public URL it called. Behind a proxy req.url can differ, so allow an override.
  const url = process.env.WHATSAPP_WEBHOOK_URL || req.url;
  if (!twilioSignatureOk(token, url, params, req.headers.get("x-twilio-signature") ?? "")) {
    console.error("[whatsapp] bad signature for", url);
    return new Response("forbidden", { status: 403 });
  }

  let media: Media | null = null;
  const mediaUrl = params.MediaUrl0;
  if (Number(params.NumMedia) > 0 && mediaUrl) {
    try {
      // Only ever fetch from Twilio itself, and only then send our credentials.
      if (new URL(mediaUrl).hostname.endsWith(".twilio.com")) {
        const res = await fetch(mediaUrl, { headers: { Authorization: auth } });
        if (res.ok) media = { bytes: await res.arrayBuffer(), type: params.MediaContentType0 || res.headers.get("content-type") || "" };
      }
    } catch (e) {
      console.error("[whatsapp] download", (e as Error).message);
    }
  }

  const text = await answer(params.Body ?? "", media, "auto", "whatsapp");

  // Reply through the Messages API: Twilio's trial "Try out WhatsApp" does not accept a TwiML reply in the response.
  // From/To are swapped from the incoming message, so the reply goes back to whoever wrote and from the number they wrote to.
  if (/^whatsapp:\+\d{6,15}$/.test(params.From ?? "") && /^whatsapp:\+\d{6,15}$/.test(params.To ?? "")) {
    try {
      const res = await fetch(`https://api.twilio.com/2010-04-01/Accounts/${encodeURIComponent(sid)}/Messages.json`, {
        method: "POST",
        headers: { Authorization: auth, "content-type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ From: params.To, To: params.From, Body: text }),
      });
      if (!res.ok) console.error("[whatsapp] send failed:", res.status, (await res.text()).slice(0, 200));
    } catch (e) {
      console.error("[whatsapp] send failed:", (e as Error).message);
    }
  }
  return new Response(null, { status: 204 });
}
