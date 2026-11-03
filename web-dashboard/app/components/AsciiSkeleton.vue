<script setup lang="ts">
import { animateSkeletons, asciiSkeletonHTML, SKELETON_CSS } from 'ascii-reveal/core'

const props = withDefaults(defineProps<{ rows?: number }>(), { rows: 3 })
const element = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()

// Markup hanya dari pola bawaan paket; tidak pernah memasukkan data API ke v-html.
const markup = computed(() => asciiSkeletonHTML({ variant: 'dither', rows: props.rows, css: false }))
useHead({ style: [{ key: 'ascii-skeleton', innerHTML: SKELETON_CSS }] })

// Canvas baru aktif di browser, lalu dibersihkan saat data siap atau animasi dikurangi.
watch([element, markup, reducedMotion, asciiAnimations], ([root, , motion, animations], _previous, onCleanup) => {
  if (root && motion !== 'reduce' && animations) {
    onCleanup(animateSkeletons(root))
  }
}, { flush: 'post' })
</script>

<template>
  <!-- eslint-disable vue/no-v-html -- HTML pola dari paket, tanpa data pengguna. -->
  <div
    ref="element"
    class="overflow-hidden font-mono text-muted"
    aria-hidden="true"
    v-html="markup"
  />
  <!-- eslint-enable vue/no-v-html -->
</template>

<style scoped>
/* Pola dasar cukup kontras di tema terang maupun gelap saat menunggu data. */
:deep(.ars-dither) {
  --ars-base: 0.42;
  --ars-wave: 0.95;
  --ars-speed: 1.2s;
}
</style>
