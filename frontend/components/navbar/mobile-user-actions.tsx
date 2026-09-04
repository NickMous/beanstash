"use client"

import {useQuery} from "@tanstack/react-query";
import {whoAmIQueryOptions} from "@/api/tanstack-query-config/userApi";
import {Link} from "@/i18n/navigation";
import {useTranslations} from "next-intl";
import {AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion";

export function MobileUserActions() {
    const tAuth = useTranslations("auth");
    const {data: user, isPending} = useQuery(whoAmIQueryOptions());

    if (isPending || user === null || user === undefined) {
        return (
            <span>
                <Link href="/login">
                    {tAuth('log_in')}
                </Link> {tAuth('or')} <Link href="/signup">
                    {tAuth('sign_up')}
                </Link>
            </span>
        )
    }

    return (
        <AccordionItem>
            <AccordionTrigger>
                {user.username}
            </AccordionTrigger>
            <AccordionContent>
                <button>{tAuth('log_out')}</button>
            </AccordionContent>
        </AccordionItem>
    )
}
