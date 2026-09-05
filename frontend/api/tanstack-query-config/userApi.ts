import {queryOptions} from "@tanstack/react-query";
import {ResponseError} from "@/generated/api";
import {userApi} from "@/app/apiClient";

// The current user. A 401 means "not logged in" — a valid state, not a failure —
// so it resolves to `null` instead of throwing. Any other error still surfaces.
export function whoAmIQueryOptions() {
    return queryOptions({
        queryKey: ["whoAmI"],
        queryFn: async ({signal}) => {
            try {
                return await userApi.whoAmI({signal});
            } catch (error) {
                if (error instanceof ResponseError && error.response.status === 403) {
                    return null;
                }
                throw error;
            }
        },
    });
}
