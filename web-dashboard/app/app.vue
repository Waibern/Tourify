<script setup lang="ts">
import { id } from '@nuxt/ui/locale'

const colorMode = useColorMode()

const color = computed(() => colorMode.value === 'dark' ? '#1b1718' : 'white')

useHead({
  meta: [
    { charset: 'utf-8' },
    { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
    { key: 'theme-color', name: 'theme-color', content: color }
  ],
  link: [
    { rel: 'icon', href: '/favicon.ico' }
  ],
  htmlAttrs: {
    lang: 'id'
  }
})

// Judul dan warna browser mengikuti dashboard admin dan petugas.
const title = 'Tourify | Dashboard'
const description = 'Dashboard admin Tourify untuk mengelola wahana dan memindai tiket pengunjung.'

useSeoMeta({
  title,
  description,
  ogTitle: title,
  ogDescription: description,
  twitterCard: 'summary_large_image'
})
</script>

<template>
  <!-- Label bawaan aplikasi, termasuk Notifikasi (F8), ditampilkan dalam bahasa Indonesia. -->
  <UApp :locale="id" :toaster="{ position: 'bottom-right', max: 3 }">
    <NuxtLoadingIndicator />

    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
