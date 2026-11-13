// Reads the text in a shared screenshot (used by the Android app). Body: the image bytes, Content-Type image/*.
import { imageToText, MAX_MEDIA } from "@/lib/bot";

export const runtime = "nodejs";
export const maxDuration = 30;

export async function POST(req: Request) {
  const type = (req.headers.get("content-type") ?? "").split(";")[0].trim();
  if (!/^image\/(jpeg|png|webp)$/.test(type)) return Response.json({ error: "Send a JPEG, PNG or WebP image." }, { status: 415 });
  const bytes = await req.arrayBuffer();
  if (bytes.byteLength === 0) return Response.json({ error: "The image was empty." }, { status: 400 });
  if (bytes.byteLength > MAX_MEDIA) return Response.json({ error: "That image is too large." }, { status: 413 });
  try {
    const text = await imageToText({ bytes, type });
    if (text.length < 5) return Response.json({ error: "No readable text found in that image." }, { status: 422 });
    return Response.json({ text: text.slice(0, 4000) });
  } catch (e) {
    console.error("[read-image]", (e as Error).message);
    return Response.json({ error: "Could not read that image. Try again." }, { status: 502 });
  }
}
