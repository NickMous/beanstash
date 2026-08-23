import {createI18n} from "@nickmous/astro-i18n";
import en from './en/_index';
import nl from './nl/_index';

export const {
    useTranslations,
} = createI18n({
    languages: { nl, en },
    defaultLanguage: 'nl',
    languageNames: { nl: 'Nederlands', en: 'English' },
});
