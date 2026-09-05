import {AuthControllerApi, Configuration, HomeControllerApi, UserControllerApi} from "@/generated/api";
import {SecurityApi} from "@/api/custom/securityApi";

// Configured entrypoint to the generated client. `generated/api` is wiped on every
// regen, so all custom configuration lives here instead.

const basePath = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

// The backend holds the CSRF token in the session; the SPA reads it from
// /auth/csrf, keeps it in memory, and sends it on unsafe requests.
const CSRF_PATH = "/api/v1/auth/csrf";
const LOGOUT_PATH = "/logout";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

let csrfToken: string | null = null;
let csrfHeaderName = "X-XSRF-TOKEN";
let refreshInFlight: Promise<void> | null = null;

function isSafe(init: RequestInit): boolean {
  return SAFE_METHODS.has((init.method ?? "GET").toUpperCase());
}

async function refreshCsrfToken(): Promise<void> {
  refreshInFlight ??= (async () => {
    try {
      const response = await fetch(basePath + CSRF_PATH, {credentials: "include"});
      if (response.ok) {
        const body = (await response.json()) as {token: string; headerName?: string};
        csrfToken = body.token;
        csrfHeaderName = body.headerName ?? csrfHeaderName;
      }
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

const configuration = new Configuration({
  basePath,
  credentials: "include",
  middleware: [
    {
      pre: async (context) => {
        if (isSafe(context.init) || context.url.endsWith(CSRF_PATH)) {
          return context;
        }
        if (!csrfToken) {
          await refreshCsrfToken();
        }
        if (csrfToken) {
          context.init.headers = {
            ...context.init.headers,
            [csrfHeaderName]: csrfToken,
          };
        }
        return context;
      },
      post: async (context) => {
        if (context.url.endsWith(LOGOUT_PATH) && context.response.ok) {
          // Logout dropped the session and its token — discard ours and pre-fetch.
          csrfToken = null;
          void refreshCsrfToken();
          return context.response;
        }
        if (
          context.response.status !== 403 ||
          isSafe(context.init) ||
          context.url.endsWith(CSRF_PATH)
        ) {
          return context.response;
        }
        // Stale token: refresh and replay once via raw fetch (no middleware, no loop).
        csrfToken = null;
        await refreshCsrfToken();
        if (!csrfToken) {
          return context.response;
        }
        return fetch(context.url, {
          ...context.init,
          headers: {
            ...context.init.headers,
            [csrfHeaderName]: csrfToken,
          },
        });
      },
    },
  ],
});

export const authApi = new AuthControllerApi(configuration);
export const homeApi = new HomeControllerApi(configuration);
export const userApi = new UserControllerApi(configuration);
export const securityApi = new SecurityApi(configuration);
