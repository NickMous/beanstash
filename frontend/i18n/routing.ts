import {defineRouting} from 'next-intl/routing';

export const routing = defineRouting({
    locales: ['en', 'nl'],
    defaultLocale: 'en',
    domains: [
        {
            domain: 'beanstash.com',
            defaultLocale: 'en',
            locales: ['en'],
        },
        {
            domain: 'beanstash.nl',
            defaultLocale: 'nl',
            locales: ['nl']
        }
    ],
    localePrefix: 'never',
});
