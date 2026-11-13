import { getRadar } from "@/lib/radar";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// Never falls back to sample data: the Radar only ever shows real checks.
export async function GET() {
  return Response.json(await getRadar(), { headers: { "cache-control": "no-store" } });
}
