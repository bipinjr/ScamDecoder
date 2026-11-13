// Telegram bot webhook. Register it once with setWebhook (see README) using TELEGRAM_WEBHOOK_SECRET.
import { answer, secretOk, MAX_MEDIA, type Media } from "@/lib/bot";

export const runtime = "nodejs";
export const maxDuration = 30;

// Telegram tells us the app language of the sender; anything else replies in the message's own language.
const LANG: Record<string, string> = {
  hi: "Hindi", kn: "Kannada", ta: "Tamil", te: "Telugu", bn: "Bengali", mr: "Marathi", ml: "Malayalam",
  gu: "Gujarati", pa: "Punjabi", or: "Odia", ur: "Urdu", as: "Assamese", ne: "Nepali",
};

type TgFile = { file_id: string; file_size?: number; mime_type?: string };
type TgMessage = {
  chat: { id: number }; message_id: number; from?: { language_code?: string };
  text?: string; caption?: string; voice?: TgFile; audio?: TgFile; photo?: TgFile[]; document?: TgFile;
};

async function download(f: TgFile, fallbackType: string): Promise<Media | null> {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) return null;
  if ((f.file_size ?? 0) > MAX_MEDIA) return { bytes: new ArrayBuffer(MAX_MEDIA + 1), type: fallbackType }; // let answer() say "too big" without downloading
  const meta = await (await fetch(`https://api.telegram.org/bot${token}/getFile?file_id=${encodeURIComponent(f.file_id)}`)).json();
  const path = meta?.result?.file_path;
  if (typeof path !== "string") return null;
  const res = await fetch(`https://api.telegram.org/file/bot${token}/${path}`);
  return res.ok ? { bytes: await res.arrayBuffer(), type: f.mime_type || fallbackType } : null;
}

export async function POST(req: Request) {
  if (!secretOk(req.headers.get("x-telegram-bot-api-secret-token"), process.env.TELEGRAM_WEBHOOK_SECRET))
    return new Response("forbidden", { status: 403 });

  const update = await req.json().catch(() => null);
  const m: TgMessage | undefined = update?.message;
  if (!m?.chat?.id) return Response.json({ ok: true }); // edits, joins, etc: nothing to answer

  let media: Media | null = null;
  try {
    if (m.voice) media = await download(m.voice, "audio/ogg");
    else if (m.audio) media = await download(m.audio, "audio/mpeg");
    else if (m.photo?.length) media = await download(m.photo[m.photo.length - 1], "image/jpeg"); // last = largest size
    else if (m.document?.mime_type?.startsWith("image/") || m.document?.mime_type?.startsWith("audio/")) media = await download(m.document, m.document.mime_type);
  } catch (e) {
    console.error("[telegram] download", (e as Error).message);
  }

  const text = await answer(m.text ?? m.caption ?? "", media, LANG[m.from?.language_code ?? ""] ?? "auto", "telegram");
  // Telegram lets a webhook reply by returning the API call as the response body: no second request needed.
  return Response.json({ method: "sendMessage", chat_id: m.chat.id, text, reply_parameters: { message_id: m.message_id }, link_preview_options: { is_disabled: true } });
}
