import {AuthControllerApi, Configuration, HomeControllerApi, UserControllerApi} from "@/generated/api";
import {SecurityApi} from "@/api/custom/securityApi";

function getCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find(row => row.startsWith(name + '='))
    ?.split('=')[1];
}

// Hand-written, configured entrypoint to the generated OpenAPI client.
// The `generated/api` folder is regenerated from the backend spec (and wiped on
// each regen), so keep custom configuration here instead.
//
// `credentials: "include"` sends the session cookie the backend sets on
// /auth/login, so authenticated calls (e.g. homeApi.home()) work after login.
const configuration = new Configuration({
  basePath: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  credentials: "include",
  middleware: [
    {
      pre: async (context) => {
        const token = getCookie("XSRF-TOKEN");
        if (token) {
          context.init.headers = {
            ...context.init.headers,
            "X-XSRF-TOKEN": token,
          };
        }

        // Add any custom headers or logging here if needed
        return context;
      }}]
});

export const authApi = new AuthControllerApi(configuration);
export const homeApi = new HomeControllerApi(configuration);
export const userApi = new UserControllerApi(configuration);
export const securityApi = new SecurityApi(configuration);
