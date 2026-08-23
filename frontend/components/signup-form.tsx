"use client"

import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Field, FieldDescription, FieldGroup, FieldLabel, FieldSeparator} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {useEffect, useState} from "react";
import {useTranslations} from "@/lang/utils";
import {authApi} from "@/app/apiClient";
import {
    startRegistration,
    type PublicKeyCredentialCreationOptionsJSON,
} from "@simplewebauthn/browser";
import {Skeleton} from "@/components/ui/skeleton";
import {useQRCode} from "next-qrcode";

// Minimum password length enforced by the backend (RegisterRequest @Size(min = 12)).
const MIN_PASSWORD_LENGTH = 12;

const LOCALSTORAGE_USERNAME_KEY = "rmb_username";
const LOCALSTORAGE_EMAIL_ADDRESS_KEY = "rmb_email";
const LOCALSTORAGE_TOTP_AUTH_URI_KEY = "rmb_totp_auth_uri";

enum SignupMethod {
    Unspecified = "unspecified",
    Passkey = "passkey",
    Totp = "totp",
}

export function SignupForm({
                               className,
                               ...props
                           }: React.ComponentProps<"div">) {
    const { SVG } = useQRCode();

    const [username, setUsername] = useState("");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [signupMethod, setSignupMethod] = useState<SignupMethod>(SignupMethod.Unspecified);
    const [signupButtonsDisabled, setSignupButtonsDisabled] = useState(false);

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const [totpVerificationNumber, setTotpVerificationNumber] = useState("");
    const [totpAuthUri, setTotpAuthUri] = useState<string | null>(null);

    const t = useTranslations("en", "auth");

    const commonFilled = Boolean(username && email && firstName && lastName);

    useEffect(() => {
        if (typeof window === 'undefined') {
            return;
        }

        const storedUsername = localStorage.getItem(LOCALSTORAGE_USERNAME_KEY);
        const storedEmail = localStorage.getItem(LOCALSTORAGE_EMAIL_ADDRESS_KEY);
        const storedTotpAuthUri = localStorage.getItem(LOCALSTORAGE_TOTP_AUTH_URI_KEY);

        if (storedUsername !== null && storedTotpAuthUri !== null) {
            setUsername(storedUsername);
            setTotpAuthUri(storedTotpAuthUri);

            if (storedEmail !== null) {
                setEmail(storedEmail);
            }

            const registerForm = document.getElementById("registerForm");
            const totpForm = document.getElementById("totpForm");

            if (registerForm === null || totpForm === null) {
                throw new Error("registerForm or totpForm not found");
            }

            registerForm.classList.toggle("opacity-0");
            registerForm.classList.toggle("opacity-100");
            registerForm.classList.toggle("hidden");
            totpForm.classList.toggle("hidden");
            totpForm.classList.toggle("opacity-100");
            totpForm.classList.toggle("opacity-0");
        }
    }, [])

    async function handleTotpSignup() {
        setSignupMethod(SignupMethod.Totp);
        setErrorMessage(null);
    }

    async function handlePasskeySignup() {
        setSignupMethod(SignupMethod.Passkey);
        setSignupButtonsDisabled(true);
        setErrorMessage(null);

        try {
            const options = await authApi.passkeyRegistrationOptions({
                passkeyRegistrationOptionsRequest: {username, email, firstName, lastName},
            });

            const attResp = await startRegistration({
                optionsJSON: options as unknown as PublicKeyCredentialCreationOptionsJSON,
            });

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
        } catch (error) {
            if (error instanceof Error && error.name === "InvalidStateError") {
                setErrorMessage(t('passkey.already-registered'));
            } else {
                setErrorMessage(t('passkey.registration-failed'));
            }
            console.error("Passkey registration failed", error);
            setSignupButtonsDisabled(false);
            return;
        }

        setSuccessMessage(t('registration-success'));
    }

    function handleSubmit(event: React.SubmitEvent<HTMLFormElement>) {
        // Navigation happens via the method buttons below; just stop Enter from reloading.
        event.preventDefault();
    }

    function retrieveTotp() {
        authApi.register({
            registerRequest: {
                username: username,
                email: email,
                firstName: firstName,
                lastName: lastName,
                password: password,
            }
        }).then(r => {
            if (r.otpAuthUri === undefined) {
                throw new Error("otpAuthUri is undefined");
            }

            setTotpAuthUri(r.otpAuthUri);
            localStorage.setItem(LOCALSTORAGE_USERNAME_KEY, username);
            localStorage.setItem(LOCALSTORAGE_EMAIL_ADDRESS_KEY, email);
            localStorage.setItem(LOCALSTORAGE_TOTP_AUTH_URI_KEY, r.otpAuthUri);
        })

        const registerForm = document.getElementById("registerForm");
        const totpForm = document.getElementById("totpForm");

        if (registerForm === null || totpForm === null) {
            throw new Error("registerForm or totpForm not found");
        }

        registerForm.addEventListener("transitionend", () => {
            registerForm.addEventListener("transitionend", () => {});

            registerForm.classList.toggle("hidden");
            totpForm.classList.toggle("hidden");
            totpForm.classList.toggle("opacity-100");
            totpForm.classList.toggle("opacity-0");
        })

        registerForm.classList.toggle("opacity-0");
        registerForm.classList.toggle("opacity-100");
    }

    function verifyTotp() {
        // Don't forget to clear the localstorage
    }

    return (
        <div className={cn("flex flex-col gap-6", className)} {...props}>
            <form onSubmit={handleSubmit} className={cn("transition-opacity duration-300 opacity-100")} id="registerForm">
                <FieldGroup>
                    <div className="flex flex-col items-center gap-2 text-center">
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
                    {successMessage && (
                        <FieldDescription className="text-center text-emerald-400">
                            {successMessage}
                        </FieldDescription>
                    )}
                    {signupMethod === SignupMethod.Totp && (
                        <>
                            <FieldSeparator />
                            <Field>
                                <FieldLabel htmlFor="password">{t('password')}</FieldLabel>
                                <Input
                                    id="password"
                                    type="password"
                                    placeholder={t('very-secure-password')}
                                    autoComplete="password"
                                    minLength={MIN_PASSWORD_LENGTH}
                                    required
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />
                            </Field>
                            <Field>
                                <Button
                                    variant="default"
                                    type="button"
                                    disabled={!password}
                                    onClick={retrieveTotp}
                                >
                                    {t('retrieve-totp-code')}
                                </Button>
                            </Field>
                        </>
                    )}
                </FieldGroup>
            </form>
            {/*<FieldDescription className="px-6 text-center">*/}
            {/*  By clicking continue, you agree to our <a href="#">Terms of Service</a>{" "}*/}
            {/*  and <a href="#">Privacy Policy</a>.*/}
            {/*</FieldDescription>*/}
            <form onSubmit={handleSubmit} className={cn("transition-opacity duration-300 opacity-0 hidden")} id="totpForm">
                <FieldGroup>
                    <div className="flex flex-col items-center gap-2 text-center">
                        <h1 className="text-xl font-bold">{t('setup-totp-title')}</h1>
                        <FieldDescription>
                            {t('setup-totp-description', {email: email})}
                        </FieldDescription>
                    </div>
                    {totpAuthUri !== null ? (
                        <SVG
                            text={'https://github.com/bunlong/next-qrcode'}
                            options={{
                                margin: 2,
                            }}
                        />
                    ) : (
                        <Skeleton className="aspect-square" />
                    )}
                    <FieldSeparator />
                    <Field>
                        <FieldLabel htmlFor="totpVerification">{t('verify-the-totp-code')}</FieldLabel>
                        <Input
                            id="totpVerification"
                            type="number"
                            minLength={6}
                            maxLength={6}
                            value={totpVerificationNumber}
                            onChange={(e) => setTotpVerificationNumber(e.target.value)}
                        />
                    </Field>
                    <Field>
                        <Button
                            variant="default"
                            type="submit"
                            disabled={totpVerificationNumber.length !== 6}
                            onClick={verifyTotp}
                        >
                            {t('verify-totp')}
                        </Button>
                    </Field>
                </FieldGroup>
            </form>
        </div>
    )
}
