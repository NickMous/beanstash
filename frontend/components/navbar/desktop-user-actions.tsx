"use client"

import {useTranslations} from "next-intl";
import {useQuery} from "@tanstack/react-query";
import {whoAmIQueryOptions} from "@/api/tanstack-query-config/userApi";
import {
    NavigationMenuContent,
    NavigationMenuItem, NavigationMenuLink,
    NavigationMenuTrigger,
    navigationMenuTriggerStyle
} from "@/components/ui/navigation-menu";
import {Link} from "@/i18n/navigation";

export function DesktopUserActions() {
    const tAuth = useTranslations("auth");
    const {data: user, isPending} = useQuery(whoAmIQueryOptions());

    if (isPending || user === null || user === undefined) {
        return (
            <NavigationMenuItem className={navigationMenuTriggerStyle()}>
                <NavigationMenuTrigger>
                    {tAuth('anonymous_user')}
                </NavigationMenuTrigger>
                <NavigationMenuContent>
                    <NavigationMenuLink href="/login">
                        {tAuth('log_in')}
                    </NavigationMenuLink>
                    <NavigationMenuLink href="/signup">
                        {tAuth('sign_up')}
                    </NavigationMenuLink>
                </NavigationMenuContent>
            </NavigationMenuItem>
        );
    }
}
