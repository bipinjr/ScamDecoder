import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // ESLint is not installed (kept the install small); tsc still type-checks the build.
  eslint: { ignoreDuringBuilds: true },
  devIndicators: false, // the dev badge sat over the bottom-left buttons in previews
};

export default nextConfig;
