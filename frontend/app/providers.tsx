"use client";

import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import {ReactQueryDevtools} from "@tanstack/react-query-devtools";

function makeQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: {
                // Trust cached data for a minute before refetching it in the background.
                staleTime: 60 * 1000,
            },
        },
    });
}

let browserQueryClient: QueryClient | undefined;

// The server must never reuse a client between requests (one user's data would
// leak into another's render). The browser keeps a single client for the life of
// the tab so the cache survives route changes and re-renders.
function getQueryClient() {
    if (typeof window === "undefined") {
        return makeQueryClient();
    }

    if (!browserQueryClient) {
        browserQueryClient = makeQueryClient();
    }

    return browserQueryClient;
}

export function Providers({children}: { children: React.ReactNode }) {
    const queryClient = getQueryClient();

    return (
        <QueryClientProvider client={queryClient}>
            {children}
            {/* Dev-only: the package no-ops this in production builds. */}
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
