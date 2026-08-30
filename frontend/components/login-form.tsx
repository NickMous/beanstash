"use client"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {KeyRound} from "lucide-react"
import Link from "next/link";
import {useTranslations} from "@/lang/utils";

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const t = useTranslations("en", "auth");

  return (
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
            <Button variant="outline" type="button">
              <KeyRound />
              {t('log_in_with_passkey')}
            </Button>
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
  )
}
