import { withSentryConfig } from "@sentry/nextjs";
import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const nextConfig: NextConfig = {
  output: "standalone",
};

const sentryConfig = withSentryConfig(nextConfig, {
  // For all available options, see:
  // https://www.npmjs.com/package/@sentry/webpack-plugin#options

  org: "beanstash",

  project: "beanstash-prod-frontend",
  sentryUrl: "https://sentry.nickmous.nl/",

  // Only print logs for uploading source maps in CI
  silent: !process.env.CI,

  // For all available options, see:
  // https://docs.sentry.io/platforms/javascript/guides/nextjs/manual-setup/

  // Source map injection + upload runs in Next.js' `runAfterProductionCompile`
  // hook and uploads every map to the self-hosted Sentry, which adds ~1min to a
  // clean build. Only do it in CI so local `next build` stays fast.
  sourcemaps: {
    disable: !process.env.CI,
    deleteSourcemapsAfterUpload: true,
  },

  // Upload a larger set of source maps for prettier stack traces (increases build time)
  widenClientFileUpload: true,

  // Bundler-agnostic replacement for the old `webpack.treeshake` block (this app
  // builds with Turbopack, where `webpack` options are ignored).
  bundleSizeOptimizations: {
    excludeDebugStatements: true,
  },

  // Route browser requests to Sentry through a Next.js rewrite to circumvent ad-blockers.
  // This can increase your server load as well as your hosting bill.
  // Note: Check that the configured route will not match with your Next.js middleware, otherwise reporting of client-
  // side errors will fail.
  tunnelRoute: "/monitoring",
});

const withNextIntl = createNextIntlPlugin();
export default withNextIntl(sentryConfig);
