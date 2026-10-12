// This file configures the initialization of Sentry on the server.
// The config you add here will be used whenever the server handles a request.
// https://docs.sentry.io/platforms/javascript/guides/nextjs/

import * as Sentry from "@sentry/nextjs";

Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,

  // Define how likely traces are sampled. Adjust this value in production, or use tracesSampler for greater control.
  tracesSampleRate: 0.2,

  // `sendDefaultPii: true` was removed in SDK v11: the default `dataCollection`
  // collects user info, cookies, headers and bodies, matching the old behavior.
  // https://docs.sentry.io/platforms/javascript/guides/nextjs/configuration/options/#dataCollection
});
