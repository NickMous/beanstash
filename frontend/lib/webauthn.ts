import {WebAuthnError} from "@simplewebauthn/browser"

// True when the ceremony ended without anything going wrong: the user closed the
// browser prompt or it timed out (NotAllowedError), or we aborted it ourselves, e.g.
// the passkey button replacing a pending autofill request or the page unmounting.
export function isWebAuthnCancellation(error: unknown) {
  return error instanceof WebAuthnError
    && (error.code === "ERROR_CEREMONY_ABORTED" || error.name === "NotAllowedError")
}
