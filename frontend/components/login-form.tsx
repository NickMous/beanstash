"use client"

import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldSeparator,} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {KeyRound} from "lucide-react"
import Link from "next/link";
import {useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {authApi, securityApi} from "@/app/apiClient";
import {
    AuthenticationResponseJSON,
    type PublicKeyCredentialRequestOptionsJSON,
    startAuthentication
} from "@simplewebauthn/browser";
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

    function loginWithPasskey() {
        setPasskeyErrorMessage(null);
        setLoadingScreenText(t('follow_browser_instructions'));

        securityApi.passkeyOptions()
            .then((resp) => startAuthentication({
                optionsJSON: resp as unknown as PublicKeyCredentialRequestOptionsJSON
            }))
            .then((resp) => securityApi.passkeyLogin(
                    resp as unknown as AuthenticationResponseJSON
                )
                    .then(() => {
                        queryClient.invalidateQueries(whoAmIQueryOptions());
                        console.log('Redirect user to home');
                    })
                    .catch(() => {
                        setPasskeyErrorMessage(t('user_not_found'))
                    })
            )
            .catch(() => {
                setPasskeyErrorMessage(t('something_unexpected_happened'))
            })
            .finally(() => setLoadingScreenText(null))
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
                        <Field>
                            <FieldLabel htmlFor="username">{t('username_or_email')}</FieldLabel>
                            <Input
                                id="username"
                                type="text"
                                placeholder="m@example.com"
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
