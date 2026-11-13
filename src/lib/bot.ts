// Shared brain for the chat bots (Telegram, WhatsApp): turn whatever was forwarded into text,
// run the same check as the web app, and write the verdict as a plain chat message.
import { groq } from "./groq";
import { checkText, MAX_CHARS } from "./check";
import { formatReply, HELP } from "./bot-core";
import { recordCheck } from "./radar";
export { twilioSignatureOk, secretOk } from "./bot-core";

export const MAX_MEDIA = 4 * 1024 * 1024; // same cap as the web upload
const VISION_MODEL = "qwen/qwen3.8-27b";

export type Media = { bytes: ArrayBuffer; type: string };

export async function imageToText(m: Media): Promise<string> {
  const url = `data:${m.type};base64,${Buffer.from(m.bytes).toString("base64")}`;
  const res = await groq.chat.completions.create({
    model: VISION_MODEL,
    temperature: 0,
    max_tokens: 1200,
    messages: [{
      role: "user",
      content: [
        // The picture is untrusted: we only ask for a transcript, and the transcript is then checked as data.
        { type: "text", text: "Copy out all the text visible in this image, exactly as written, in its original language. Rejoin any word or web link that is split across two lines. Output only that text. If there is no text, output nothing." },
        { type: "image_url", image_url: { url } },
      ],
    }],
  });
  return res.choices[0]?.message?.content?.trim() ?? "";
}

async function audioToText(m: Media): Promise<string> {
  const ext = m.type.includes("mpeg") ? "mp3" : m.type.includes("mp4") || m.type.includes("m4a") ? "m4a" : m.type.includes("wav") ? "wav" : "ogg";
  const res = await groq.audio.transcriptions.create({ file: new File([m.bytes], `note.${ext}`, { type: m.type }), model: "whisper-large-v3-turbo", temperature: 0 });
  return res.text?.trim() ?? "";
}

// The one entry point both bots call. Never throws: a bot must always answer something.
export async function answer(text: string, media: Media | null, language = "auto", channel = "other"): Promise<string> {
  try {
    let input = text.trim(), heard = "";
    if (media) {
      if (media.bytes.byteLength > MAX_MEDIA) return "That file is too big for me. Send a shorter voice note (under 3 minutes) or a smaller picture.";
      const isImage = media.type.startsWith("image/"), isAudio = media.type.startsWith("audio/") || media.type === "video/webm";
      if (!isImage && !isAudio) return "I can read text messages, screenshots and voice notes. Forward one of those.";
      heard = isImage ? await imageToText(media) : await audioToText(media);
      if (heard.length < 5) return isImage ? "I could not find any readable text in that picture. Try a sharper screenshot." : "I could not hear any words in that voice note. Try a clearer recording.";
      input = `${heard}\n${input}`.trim();
    }
    if (input.length < 5 || /^\/(start|help)\b/i.test(input) || /^(hi|hello|hey|namaste)\W*$/i.test(input)) return HELP;
    const a = await checkText(input.slice(0, MAX_CHARS), language);
    const reported = await recordCheck(input, a, channel, language === "auto" ? "Auto" : language);
    return formatReply(a, heard) + (reported ? "\n\nAdded anonymously to the DECODR Scam Radar to warn others." : "");
  } catch (e) {
    console.error("[bot]", (e as Error).message);
    return "I could not read that just now. Please send it again in a minute. If someone is asking for money or an OTP, do not pay or share it. Helpline: 1930";
  }
}
