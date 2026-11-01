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
    }
  },

  compatibilityDate: '2026-06-30',

  // API PHP diproxy agar URL localhost dan cookie login tetap satu origin di HP.
  nitro: {
    devProxy: {
      '/tourify-api': {
        target: 'http://127.0.0.1:8888/tourify-api',
        changeOrigin: true,
        headers: { origin: 'http://localhost:3000' }
      }
    }
  },

  eslint: {
    config: {
      stylistic: {
        commaDangle: 'never',
        braceStyle: '1tbs'
      }
    }
  }
})
