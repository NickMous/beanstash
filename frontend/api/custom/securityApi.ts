import {BaseAPI} from "@/generated/api";

// Endpoints served by Spring Security's filters rather than a @RestController,
// so springdoc never sees them and the generated client has no methods for them.
export class SecurityApi extends BaseAPI {
  async passkeyOptions(): Promise<PublicKeyCredentialRequestOptionsJSON> {
    const response = await this.request({
      path: "/webauthn/authenticate/options",
      method: "POST",
      headers: {},
    });

    return response.json();
  }

  async passkeyLogin(credential: AuthenticationResponseJSON): Promise<void> {
    await this.request({
      path: "/login/webauthn",
      method: "POST",
      // Without a JSON Content-Type the runtime skips JSON.stringify().
      headers: {"Content-Type": "application/json"},
      body: credential,
    });
  }

  async logout(): Promise<void> {
    await this.request({
      path: "/logout",
      method: "POST",
      headers: {},
    });
  }
}
