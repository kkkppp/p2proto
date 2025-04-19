import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import HttpApi from 'i18next-http-backend';

i18n
    .use(HttpApi)
    .use(initReactI18next)
    .init({
        lng: 'en',
        fallbackLng: 'en',
        ns: ['translation'], // specifying the namespace
        defaultNS: 'translation',
        backend: {
            loadPath: `${import.meta.env.BASE_URL}locales/{{lng}}/{{ns}}.json`
        },
        interpolation: {
            escapeValue: false, // React already escapes values
        },
    });

export default i18n;