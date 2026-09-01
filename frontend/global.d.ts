import type en from "@/messages/en/_index";

declare module "next-intl" {
    interface AppConfig {
        Messages: typeof en;
        Locale: "en" | "nl";
    }
}
