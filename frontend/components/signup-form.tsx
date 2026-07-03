"use client"

import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Field, FieldDescription, FieldGroup, FieldLabel, FieldSeparator} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {useState} from "react";
import {useTranslations} from "@/lang/utils";
import {authApi} from "@/app/apiClient";
import {
    startRegistration,
    type PublicKeyCredentialCreationOptionsJSON,
} from "@simplewebauthn/browser";

// Minimum password length enforced by the backend (RegisterRequest @Size(min = 12)).
const MIN_PASSWORD_LENGTH = 12;

enum SignupMethod {
    Unspecified = "unspecified",
    Passkey = "passkey",
    Totp = "totp",
}

export function SignupForm({
                               className,
                               ...props
                           }: React.ComponentProps<"div">) {
    const [username, setUsername] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [signupMethod, setSignupMethod] = useState<SignupMethod>(SignupMethod.Unspecified);
    const [signupButtonsDisabled, setSignupButtonsDisabled] = useState(false);

    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const t = useTranslations("en", "auth");

    // Fields required by both signup paths (username/email/name). Passkey signup is
    // passwordless (/register/passkey/*); only the TOTP path (/register) needs a password.
    const commonFilled = Boolean(username && email && firstName && lastName);

    async function handleTotpSignup() {
        setSignupMethod(SignupMethod.Totp);
    }

    async function handlePasskeySignup() {
        setSignupMethod(SignupMethod.Passkey);
        setSignupButtonsDisabled(true);
        setErrorMessage(null);

        try {
            // 1. Fetch creation options. The OpenAPI spec maps Spring's WebAuthn value types
            // (Bytes, the enum-like tokens, COSE alg) to their real scalar wire form, so the
            // generated model is a faithful options JSON — no raw response reading needed.
            const options = await authApi.passkeyRegistrationOptions({
                passkeyRegistrationOptionsRequest: {username, email, firstName, lastName},
            });

            // 2. Run the WebAuthn ceremony. The generated options object has the same runtime
            // shape SimpleWebAuthn wants; the cast only bridges the two libraries' nominal types.
            const attResp = await startRegistration({
                optionsJSON: options as unknown as PublicKeyCredentialCreationOptionsJSON,
            });

            // 3. Complete registration. Bound server-side to Spring's RelyingPartyPublicKey
            // ({ credential, label }); the options are reloaded from the HTTP session and, on
            // success, the controller opens the session (cookie stored via credentials: "include").
            // clientExtensionResults is omitted — optional and empty for a basic registration.
            await authApi.completePasskeyRegistration({
                relyingPartyPublicKey: {
                    credential: {
                        id: attResp.id,
                        type: attResp.type,
                        rawId: attResp.rawId,
                        response: {
                            clientDataJSON: attResp.response.clientDataJSON,
                            attestationObject: attResp.response.attestationObject,
                            transports: attResp.response.transports ?? [],
                        },
                        authenticatorAttachment: attResp.authenticatorAttachment,
                    },
                    label: `${username}'s passkey`,
                },
            });

            // Registered and authenticated (session cookie set).
        } catch (error) {
            if (error instanceof Error && error.name === "InvalidStateError") {
                setErrorMessage(t('passkey.already-registered'));
            } else {
                setErrorMessage(t('passkey.registration-failed'));
            }
            console.error("Passkey registration failed", error);
            setSignupButtonsDisabled(false);
        }
    }

    function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
        // Navigation happens via the method buttons below; just stop Enter from reloading.
        event.preventDefault();
    }

    return (
        <div className={cn("flex flex-col gap-6", className)} {...props}>
            <form onSubmit={handleSubmit}>
                <FieldGroup>
                    <div className="flex flex-col items-center gap-2 text-center">
                        <a
                            href="#"
                            className="flex flex-col items-center gap-2 font-medium"
                        >
                            {/*<div className="flex size-8 items-center justify-center rounded-md">*/}
                            {/*  <GalleryVerticalEndIcon className="size-6" />*/}
                            {/*</div>*/}
                            <span className="sr-only">Beanstash</span>
                        </a>
                        <h1 className="text-xl font-bold">{t('welcome-to')} Beanstash</h1>
                        <FieldDescription>
                            {t('already-have-account')} <a href="/signin">{t('sign-in')}</a>
                        </FieldDescription>
                    </div>
                    <Field>
                        <FieldLabel htmlFor="username">{t('username')}</FieldLabel>
                        <Input
                            id="username"
                            type="text"
                            autoComplete="username"
                            required
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                        />
                    </Field>
                    <div className="grid grid-cols-2 gap-4">
                        <Field>
                            <FieldLabel htmlFor="firstName">{t('first-name')}</FieldLabel>
                            <Input
                                id="firstName"
                                type="text"
                                autoComplete="given-name"
                                required
                                value={firstName}
                                onChange={(e) => setFirstName(e.target.value)}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="lastName">{t('last-name')}</FieldLabel>
                            <Input
                                id="lastName"
                                type="text"
                                autoComplete="family-name"
                                required
                                value={lastName}
                                onChange={(e) => setLastName(e.target.value)}
                            />
                        </Field>
                    </div>
                    <Field>
                        <FieldLabel htmlFor="email">{t('email')}</FieldLabel>
                        <Input
                            id="email"
                            type="email"
                            placeholder="m@example.com"
                            autoComplete="email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </Field>
                    {/*<Field>*/}
                    {/*  <FieldLabel htmlFor="password">{t('password')}</FieldLabel>*/}
                    {/*  <Input*/}
                    {/*    id="password"*/}
                    {/*    type="password"*/}
                    {/*    autoComplete="new-password"*/}
                    {/*    minLength={MIN_PASSWORD_LENGTH}*/}
                    {/*    value={password}*/}
                    {/*    onChange={(e) => setPassword(e.target.value)}*/}
                    {/*  />*/}
                    {/*  <FieldDescription>{t('password-hint')}</FieldDescription>*/}
                    {/*</Field>*/}
                    <FieldDescription className="text-center">
                        {t('choose-method')}
                    </FieldDescription>
                    <Field className="grid gap-4">
                        <Button
                            variant="default"
                            type="button"
                            disabled={!commonFilled || signupButtonsDisabled}
                            onClick={handlePasskeySignup}
                        >
                            {t('use-passkey')}
                        </Button>
                        <Button
                            variant="outline"
                            type="button"
                            disabled={!commonFilled || signupButtonsDisabled}
                            onClick={handleTotpSignup}
                        >
                            {t('use-totp')}
                        </Button>
                    </Field>
                    {errorMessage && (
                        <FieldDescription className="text-center text-destructive">
                            {errorMessage}
                        </FieldDescription>
                    )}
                    {signupMethod !== SignupMethod.Unspecified && (
                        <FieldSeparator />
                    )}
                </FieldGroup>
            </form>
            {/*<FieldDescription className="px-6 text-center">*/}
            {/*  By clicking continue, you agree to our <a href="#">Terms of Service</a>{" "}*/}
            {/*  and <a href="#">Privacy Policy</a>.*/}
            {/*</FieldDescription>*/}
        </div>
    )
}
