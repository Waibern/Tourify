// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@vueuse/nuxt'
  ],

  devtools: {
    enabled: true
  },

  css: ['~/assets/css/main.css'],

  runtimeConfig: {
    public: {
      tourifyApiBaseUrl: '/tourify-api'
    }
  },

  routeRules: {
    '/api/**': {
      cors: true
    },
    // Proxy PHP tetap satu asal saat dashboard dibuka dari HP di jaringan lokal.
    '/tourify-api/**': {
      proxy: {
        to: 'http://127.0.0.1:8888/tourify-api/**',
        headers: { origin: 'http://localhost:3000' }
      }
    }
  },

  compatibilityDate: '2026-06-30',

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  }
})
