import { AuthControllerApi, Configuration, HomeControllerApi } from "@/generated/api";

// Hand-written, configured entrypoint to the generated OpenAPI client.
// The `generated/api` folder is regenerated from the backend spec (and wiped on
// each regen), so keep custom configuration here instead.
//
// `credentials: "include"` sends the session cookie the backend sets on
// /auth/login, so authenticated calls (e.g. homeApi.home()) work after login.
const configuration = new Configuration({
  basePath: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  credentials: "include",
});

export const authApi = new AuthControllerApi(configuration);
export const homeApi = new HomeControllerApi(configuration);
