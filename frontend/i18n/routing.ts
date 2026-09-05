import {defineRouting} from 'next-intl/routing';

export const routing = defineRouting({
    locales: ['en', 'nl'],
    defaultLocale: 'en',
    domains: [
        {
            domain: 'en.beanstash.org',
            defaultLocale: 'en',
            locales: ['en'],
        },
        {
            domain: 'nl.beanstash.org',
            defaultLocale: 'nl',
            locales: ['nl']
        }
    ],
    localePrefix: 'never',
});
