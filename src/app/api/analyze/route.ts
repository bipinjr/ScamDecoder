import { checkText, MAX_CHARS } from "@/lib/check";
import { LANGUAGES } from "@/lib/shared";
import { recordCheck } from "@/lib/radar";

export const runtime = "nodejs";
export const maxDuration = 30;

export async function POST(req: Request) {
  let body: { text?: unknown; language?: unknown; channel?: unknown };
  try {
    body = await req.json();
  } catch {
    return Response.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const text = typeof body.text === "string" ? body.text.trim() : "";
  const language =
    typeof body.language === "string" && LANGUAGES.includes(body.language) ? body.language : "English";

  if (text.length < 5) return Response.json({ error: "Paste a message to analyze." }, { status: 400 });
  if (text.length > MAX_CHARS)
    return Response.json({ error: `Message too long (max ${MAX_CHARS} characters).` }, { status: 400 });

  const a = await checkText(text, language);
  const channel = typeof body.channel === "string" ? body.channel : "other";
  return Response.json({ ...a, reported: await recordCheck(text, a, channel, language) });
}
