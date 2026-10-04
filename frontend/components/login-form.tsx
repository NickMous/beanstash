"use client"

import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldSeparator,} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {KeyRound} from "lucide-react"
import Link from "next/link";
import {useEffect, useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {authApi, securityApi} from "@/app/apiClient";
import {
    type AuthenticationResponseJSON,
    browserSupportsPasskeys,
    browserSupportsWebAuthnAutofill,
    startAuthentication,
    WebAuthnAbortService,
} from "@simplewebauthn/browser";
import {isWebAuthnCancellation} from "@/lib/webauthn";
import {InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot} from "@/components/ui/input-otp";
import {useTranslations} from "next-intl";
import {useQueryClient} from "@tanstack/react-query";
import {whoAmIQueryOptions} from "@/api/tanstack-query-config/userApi";

export function LoginForm({
                              className,
                              ...props
                          }: React.ComponentProps<"div">) {
    const t = useTranslations("auth");
    const queryClient = useQueryClient();

    const [passkeyErrorMessage, setPasskeyErrorMessage] = useState<string | null>(null);
    const [totpErrorMessage, setTotpErrorMessage] = useState<string | null>(null);
    const [loadingScreenText, setLoadingScreenText] = useState<string | null>(null);

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [totpCode, setTotpCode] = useState("");

    // Optimistic so the button doesn't pop in on the (common) supported browsers.
    const [passkeysSupported, setPasskeysSupported] = useState(true);

    useEffect(() => {
        let active = true;

        browserSupportsPasskeys().then((supported) => {
            if (active) setPasskeysSupported(supported);
        });
        startPasskeyAutofill(() => active);

        return () => {
            active = false;
            WebAuthnAbortService.cancelCeremony();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps -- arm autofill once on mount
    }, []);

    async function completePasskeyLogin(credential: AuthenticationResponseJSON) {
        try {
            await securityApi.passkeyLogin(credential);
        } catch {
            setPasskeyErrorMessage(t('user_not_found'));
            return false;
        }
        queryClient.invalidateQueries(whoAmIQueryOptions());
        console.log('Redirect user to home');
        return true;
    }

    // Starts a background passkey request that surfaces the user's passkeys in the
    // username field's autofill dropdown. It stays pending until a passkey is picked,
    // or until loginWithPasskey() replaces it (only one ceremony can run at a time).
    async function startPasskeyAutofill(isActive: () => boolean = () => true) {
        if (!await browserSupportsWebAuthnAutofill()) return;

        try {
            const optionsJSON = await securityApi.passkeyOptions();
            if (!isActive()) return;

            const credential = await startAuthentication({optionsJSON, useBrowserAutofill: true});
            setPasskeyErrorMessage(null);
            if (!await completePasskeyLogin(credential)) startPasskeyAutofill(isActive);
        } catch (error) {
            if (!isWebAuthnCancellation(error)) console.error("Passkey autofill failed", error);
        }
    }

    async function loginWithPasskey() {
        setPasskeyErrorMessage(null);
        setLoadingScreenText(t('follow_browser_instructions'));

        let loggedIn = false;
        try {
            const optionsJSON = await securityApi.passkeyOptions();
            const credential = await startAuthentication({optionsJSON});
            loggedIn = await completePasskeyLogin(credential);
        } catch (error) {
            if (!isWebAuthnCancellation(error)) {
                setPasskeyErrorMessage(t('something_unexpected_happened'));
            }
        } finally {
            setLoadingScreenText(null);
        }

        // This ceremony aborted the autofill one, so re-arm it.
        if (!loggedIn) startPasskeyAutofill();
    }

    function loginWithTotp() {
        authApi.login({
            loginRequest: {
                username,
                password,
                totpCode
            }
        })
            .then(() => {
                queryClient.invalidateQueries(whoAmIQueryOptions());
                console.log("Redirect to home")
            })
            .catch(() => setTotpErrorMessage(t('something_unexpected_happened')))
    }

    return (
        <>
            <div className={cn("flex flex-col gap-6", className)} {...props}>
                <form onSubmit={(e) => e.preventDefault()}>
                    <FieldGroup>
                        <div className="flex flex-col items-center gap-2 text-center">
                            <h1 className="text-xl font-bold">{t('welcome_back')}</h1>
                            <FieldDescription>
                                {t('dont_have_an_account')} <Link href={'/signup'}>{t('sign_up')}</Link>
                            </FieldDescription>
                        </div>
                        {passkeysSupported ? (
                            <>
                                <Field>
                                    <Button
                                        variant="outline"
                                        type="button"
                                        onClick={loginWithPasskey}
                                    >
                                        <KeyRound/>
                                        {t('log_in_with_passkey')}
                                    </Button>
                                    {passkeyErrorMessage !== null ? (
                                        <FieldError>
                                            {passkeyErrorMessage}
                                        </FieldError>
                                    ) : null}
                                </Field>
                                <FieldSeparator>{t('or')}</FieldSeparator>
                            </>
                        ) : null}
                        <Field>
                            <FieldLabel htmlFor="username">{t('username_or_email')}</FieldLabel>
                            <Input
                                id="username"
                                type="text"
                                placeholder="m@example.com"
                                // "webauthn" lets the browser offer passkeys in this field's autofill.
                                autoComplete="username webauthn"
                                required
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="password">{t('password')}</FieldLabel>
                            <Input
                                id="password"
                                type="password"
                                placeholder="very-secure-password"
                                required
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </Field>
                        <Field>
                            <FieldLabel htmlFor="totpVerification">{t('totp_code')}</FieldLabel>
                            <InputOTP
                                id="totpVerification"
                                maxLength={6}
                                value={totpCode}
                                onChange={(newValue) => setTotpCode(newValue)}
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
                        {totpErrorMessage !== null ? (
                            <FieldError>{totpErrorMessage}</FieldError>
                        ) : null}
                        <Field>
                            <Button
                                type="submit"
                                onClick={loginWithTotp}
                            >
                                {t('log_in_with_totp')}
                            </Button>
                        </Field>
                    </FieldGroup>
                </form>
            </div>
            <div
                className={cn(
                    'h-screen w-screen absolute items-center justify-center top-0 left-0 right-0 animate-in fade-in duration-300 hidden bg-white/80',
                    loadingScreenText !== null ? 'flex' : ''
                )}
            >
                <Spinner className={"mr-2"}/>
                {loadingScreenText}
            </div>
        </>
    )
}
