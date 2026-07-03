import en from './en/_index';
import nl from './nl/_index';

export const languages = { en, nl };
export const defaultLanguage = 'en';
export type Language = keyof typeof languages;
export type Domain = keyof typeof en;
export type Context = Map<string, string|number>;

// Builds the union of valid dotted key paths into a translation object,
// e.g. `{ a: { b: '' }, c: '' }` -> `'a.b' | 'c'`.
type DottedKeys<T> = T extends string
    ? never
    : {
        [K in Extract<keyof T, string>]: T[K] extends string
            ? K
            : `${K}.${DottedKeys<T[K]>}`;
    }[Extract<keyof T, string>];

export type TranslationKey<D extends Domain> = DottedKeys<(typeof en)[D]>;

export function getLangFromUrl(url: URL) {
    const [, lang] = url.pathname.split('/');
    if (lang in languages) return lang as keyof typeof languages;
    return defaultLanguage;
}

export function useTranslations<D extends Domain>(lang: Language, domain: D, context: Context = new Map()) {
    const parentLanguage: Language = lang;
    const parentDomain: D = domain;
    const parentContext: Context = context;

    return function translate<D2 extends Domain = D>(
        key: TranslationKey<D2>,
        context: Context = parentContext,
        domain: D2 = parentDomain as unknown as D2,
        language: Language = parentLanguage,
    ): string {
        const keyParts = (key as string).split('.');

        if (keyParts.length === 1 && keyParts[0] === '') {
            throw new NoKeyError('Empty key provided');
        }

        let node: unknown = languages[language][domain];

        for (const part of keyParts) {
            if (typeof node !== 'object' || node === null || !(part in node)) {
                throw new KeyNotFoundError('Translation key not found: ' + key);
            }
            node = (node as Record<string, unknown>)[part];
        }

        if (typeof node !== 'string') {
            throw new KeyNotFoundError('Translation key not found: ' + key);
        }

        let translation = node;
        for (const [search, replace] of context.entries()) {
            translation = translation.split(`{${search}}`).join(String(replace));
        }

        return translation;
    }
}

export class NoKeyError extends Error {}
export class KeyNotFoundError extends Error {}
