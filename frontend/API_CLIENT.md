# API client

The backend is the single source of truth for the API. Its `@RestController`s
are introspected by **springdoc** into an OpenAPI 3 spec, and the
**openapi-generator** Maven plugin turns that spec into the typed TypeScript
client in `generated/api`.

```
controllers (Java) ─springdoc─▶ backend/openapi.json ─openapi-generator─▶ frontend/generated/api (typescript-fetch)
```

## Using it

Import the configured instances from `app/apiClient.ts` (not from
`generated/api` directly — that folder is regenerated and holds no
configuration):

```ts
import { authApi, homeApi } from "@/app/apiClient";

// Register (returns the TOTP setup payload)
const totp = await authApi.register({
  registerRequest: {
    username: "alice",
    email: "alice@example.com",
    password: "securepassword1",
    firstName: "Alice",
    lastName: "Example",
  },
});

// Log in (sets the session cookie; credentials: "include" sends it on later calls)
await authApi.login({
  loginRequest: { username: "alice", password: "securepassword1", totpCode: "123456" },
});

// Call a secured endpoint with the session cookie
const message = await homeApi.home();
```

Set `NEXT_PUBLIC_API_URL` to point at a non-local backend; it defaults to
`http://localhost:8080`.

## Regenerating after an API change

Anything that changes the API surface (new endpoint, changed DTO) requires
regenerating. From `backend/`:

```bash
./mvnw -Pgenerate-api-client verify
```

This runs the tests — which re-export `backend/openapi.json` via
`OpenApiDocsExportTests` — and then regenerates `frontend/generated/api` from
it. Commit both `backend/openapi.json` and the regenerated `frontend/generated/api`.

A plain `./mvnw verify` (no profile) still refreshes `backend/openapi.json`, so
a dirty `openapi.json` in CI means someone changed the API without regenerating
the client.

## Notes

- `generated/**` is excluded from ESLint (generated code); `app/apiClient.ts` is not.
- Swagger UI is served at `/swagger-ui.html` and the raw spec at `/v3/api-docs`.
  Disable them in production with `SPRINGDOC_API_DOCS_ENABLED=false` and
  `SPRINGDOC_SWAGGER_UI_ENABLED=false`.
