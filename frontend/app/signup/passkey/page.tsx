import Link from "next/link"
import { redirect } from "next/navigation"
// Aliased: this is a plain helper, not a React hook, so it's safe in a Server Component.
import { useTranslations as getTranslations } from "@/lang/utils"

export default async function PasskeySignupPage({
  searchParams,
}: {
  searchParams: Promise<{ email?: string }>
}) {
  const { email } = await searchParams
  if (!email) redirect("/signup")

  // No locale routing yet — fall back to the app's default language.
  const t = getTranslations("en", "auth")

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-background p-6 md:p-10">
      <div className="flex w-full max-w-sm flex-col items-center gap-4 text-center">
        <h1 className="text-xl font-bold">{t("setup-passkey-title")}</h1>
        <p className="text-sm text-muted-foreground">
          {t("setup-passkey-description", new Map([["email", email]]))}
        </p>
        <Link href="/signup" className="text-sm underline underline-offset-4">
          {t("back-to-signup")}
        </Link>
      </div>
    </div>
  )
}
