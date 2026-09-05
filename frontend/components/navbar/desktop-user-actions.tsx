"use client"

import {useTranslations} from "next-intl";
import {useQuery, useQueryClient} from "@tanstack/react-query";
import {whoAmIQueryOptions} from "@/api/tanstack-query-config/userApi";
import {
    NavigationMenuContent,
    NavigationMenuItem, NavigationMenuLink,
    NavigationMenuTrigger,
    navigationMenuTriggerStyle
} from "@/components/ui/navigation-menu";
import {securityApi} from "@/app/apiClient";

export function DesktopUserActions() {
    const tAuth = useTranslations("auth");
    const queryClient = useQueryClient();
    const {data: user, isPending} = useQuery(whoAmIQueryOptions());

    function logoutUser() {
        securityApi.logout()
            .then(() => queryClient.invalidateQueries(whoAmIQueryOptions()));
    }

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

    return (
        <NavigationMenuItem className={navigationMenuTriggerStyle()}>
            <NavigationMenuTrigger>
                {user.username}
            </NavigationMenuTrigger>
            <NavigationMenuContent>
                <NavigationMenuLink href="#" onClick={logoutUser}>
                    {tAuth('log_out')}
                </NavigationMenuLink>
            </NavigationMenuContent>
        </NavigationMenuItem>
    )
}
