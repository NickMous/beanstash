"use client"

import dynamic from "next/dynamic"
import { Skeleton } from "@/components/ui/skeleton"

// The signup form reads localStorage in its useState initializers, so it must
// never render on the server.
export const SignupFormClient = dynamic(
    () => import("@/components/signup-form").then((m) => m.SignupForm),
    { ssr: false, loading: () => <p className={'shimmer text-muted-foregrond text-center'}>Initializing page&hellip;</p> },
)
