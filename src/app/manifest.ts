import type { MetadataRoute } from "next";

// Installable app on phones. "Share → DECODR" from WhatsApp, SMS or any browser opens /share with the link, text or QR image.
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "DECODR: see through the scam",
    short_name: "DECODR",
    description: "Check links, messages, QR codes and files for scams before you tap.",
    id: "/",
    start_url: "/share",
    scope: "/",
    display: "standalone",
    background_color: "#070a14",
    theme_color: "#070a14",
    orientation: "portrait",
    icons: [
      { src: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icons/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
    share_target: {
      action: "/share-target",
      method: "POST",
      enctype: "multipart/form-data",
      params: { title: "title", text: "text", url: "url", files: [{ name: "image", accept: ["image/*"] }] },
    },
  } as MetadataRoute.Manifest;
}
