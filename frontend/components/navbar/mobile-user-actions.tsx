"use client"

import {useQuery, useQueryClient} from "@tanstack/react-query";
import {whoAmIQueryOptions} from "@/api/tanstack-query-config/userApi";
import {Link} from "@/i18n/navigation";
import {useTranslations} from "next-intl";
import {AccordionContent, AccordionItem, AccordionTrigger} from "@/components/ui/accordion";
import {securityApi} from "@/app/apiClient";

export function MobileUserActions() {
    const tAuth = useTranslations("auth");
    const queryClient = useQueryClient();
    const {data: user, isPending} = useQuery(whoAmIQueryOptions());

    function logoutUser() {
        securityApi.logout()
            .then(() => queryClient.invalidateQueries(whoAmIQueryOptions()));
    }

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
                <button onClick={logoutUser}>{tAuth('log_out')}</button>
            </AccordionContent>
        </AccordionItem>
    )
}
