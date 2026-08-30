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
import {Skeleton} from "@/components/ui/skeleton";
import {useQRCode} from "next-qrcode";
import {InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot} from "@/components/ui/input-otp";

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

enum SignupStep {
    Register = "register",
    // Register form is fading out; the TOTP step mounts once the transition ends.
    RegisterFadingOut = "register-fading-out",
    Totp = "totp",
}

export function SignupForm({
                               className,
                               ...props
                           }: React.ComponentProps<"div">) {
    const { SVG } = useQRCode();

    const [username, setUsername] = useState(() => localStorage.getItem(LOCALSTORAGE_USERNAME_KEY) ?? "");
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState(() => localStorage.getItem(LOCALSTORAGE_EMAIL_ADDRESS_KEY) ?? "");
    const [password, setPassword] = useState("");

    const [signupMethod, setSignupMethod] = useState<SignupMethod>(SignupMethod.Unspecified);
    const [signupButtonsDisabled, setSignupButtonsDisabled] = useState(false);

    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const [totpVerificationNumber, setTotpVerificationNumber] = useState("");
    const [totpAuthUri, setTotpAuthUri] = useState<string | null>(() => localStorage.getItem(LOCALSTORAGE_TOTP_AUTH_URI_KEY));

    // Resume an interrupted TOTP setup when the values above were restored from localStorage.
    const [step, setStep] = useState<SignupStep>(() =>
        username !== "" && totpAuthUri !== null && totpAuthUri !== ""
            ? SignupStep.Totp
            : SignupStep.Register
    );

    const t = useTranslations("en", "auth");

    const commonFilled = Boolean(username && email && firstName && lastName);

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
        setErrorMessage(null);
        setStep(SignupStep.RegisterFadingOut);

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
        }).catch(error => {
            console.error("TOTP registration failed", error);
            setErrorMessage(t('totp.registration-failed'));
            setStep(SignupStep.Register);
        });
    }

    function verifyTotp() {
        setErrorMessage(null);
        authApi.verifyTotp({
            verifyTotpRequest: {
                username: username,
                code: totpVerificationNumber,
            }
        })
            .then(() => {
                setSuccessMessage(t('totp_verification_completed'))
                localStorage.removeItem(LOCALSTORAGE_EMAIL_ADDRESS_KEY);
                localStorage.removeItem(LOCALSTORAGE_USERNAME_KEY);
                localStorage.removeItem(LOCALSTORAGE_TOTP_AUTH_URI_KEY);
            })
            .catch(() => {
                setErrorMessage(t('totp_verification_failed'))
            })
    }

    return (
        <div className={cn("flex flex-col gap-6", className)} {...props}>
            {step !== SignupStep.Totp && (
                <form
                    onSubmit={handleSubmit}
                    className={cn(
                        "transition-opacity duration-300",
                        step === SignupStep.RegisterFadingOut ? "opacity-0" : "opacity-100",
                    )}
                    onTransitionEnd={(e) => {
                        // Child transitions (e.g. button hovers) bubble up here too.
                        if (e.target === e.currentTarget && step === SignupStep.RegisterFadingOut) {
                            setStep(SignupStep.Totp);
                        }
                    }}
                >
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
            )}
            {step === SignupStep.Totp && (
                <form onSubmit={handleSubmit} className="animate-in fade-in duration-300">
                    <FieldGroup>
                        <div className="flex flex-col items-center gap-2 text-center">
                            <h1 className="text-xl font-bold">{t('setup-totp-title')}</h1>
                            <FieldDescription>
                                {t('setup-totp-description', {email: email})}
                            </FieldDescription>
                        </div>
                        {totpAuthUri !== null ? (
                            <>
                                <SVG
                                    text={totpAuthUri}
                                    options={{
                                        margin: 2,
                                    }}
                                />
                                {/*<p>URL: {totpAuthUri}</p>*/}
                            </>
                        ) : (
                            <Skeleton className="aspect-square" />
                        )}
                        <FieldSeparator />
                        <Field>
                            <FieldLabel htmlFor="totpVerification">{t('verify-the-totp-code')}</FieldLabel>
                            <InputOTP
                                id="totpVerification"
                                maxLength={6}
                                value={totpVerificationNumber}
                                onChange={(newValue) => setTotpVerificationNumber(newValue)}
                            >
                                <InputOTPGroup>
                                    <InputOTPSlot index={0} />
                                    <InputOTPSlot index={1} />
                                    <InputOTPSlot index={2} />
                                </InputOTPGroup>
                                <InputOTPSeparator />
                                <InputOTPGroup>
                                    <InputOTPSlot index={3} />
                                    <InputOTPSlot index={4} />
                                    <InputOTPSlot index={5} />
                                </InputOTPGroup>
                            </InputOTP>
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
            )}
        </div>
    )
}
