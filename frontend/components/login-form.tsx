"use client"

import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldSeparator,} from "@/components/ui/field"
import {Input} from "@/components/ui/input"
import {KeyRound} from "lucide-react"
import Link from "next/link";
import {useTranslations} from "@/lang/utils";
import {useState} from "react";
import {Spinner} from "@/components/ui/spinner";
import {securityApi} from "@/app/apiClient";
import {
    AuthenticationResponseJSON,
    type PublicKeyCredentialRequestOptionsJSON,
    startAuthentication
} from "@simplewebauthn/browser";

export function LoginForm({
                              className,
                              ...props
                          }: React.ComponentProps<"div">) {
    const t = useTranslations("en", "auth");

    const [passkeyErrorMessage, setPasskeyErrorMessage] = useState<string | null>(null);
    const [loadingScreenText, setLoadingScreenText] = useState<string | null>(null);

    async function loginWithPasskey() {
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

    return (
        <>
            <div className={cn("flex flex-col gap-6", className)} {...props}>
                <form>
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
                            <FieldLabel htmlFor="email">{t('username_or_email')}</FieldLabel>
                            <Input
                                id="email"
                                type="email"
                                placeholder="m@example.com"
                                required
                            />
                        </Field>
                        <Field>
                            <Button type="submit">{t('log_in_with_totp')}</Button>
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
