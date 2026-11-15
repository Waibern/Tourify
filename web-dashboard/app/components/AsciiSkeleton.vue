<script setup lang="ts">
import { animateSkeletons, asciiSkeletonHTML, SKELETON_CSS } from 'ascii-reveal/core'

const props = withDefaults(defineProps<{ rows?: number }>(), { rows: 3 })
const element = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()

// Isi HTML berasal dari pola paket, bukan dari data API.
const markup = computed(() => asciiSkeletonHTML({ variant: 'dither', rows: props.rows, css: false }))
useHead({ style: [{ key: 'ascii-skeleton', innerHTML: SKELETON_CSS }] })

// Buat kanvas di browser, lalu bersihkan saat data siap atau animasi dimatikan.
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
/* Pola tetap terlihat di tema terang dan gelap saat data dimuat. */
:deep(.ars-dither) {
  --ars-base: 0.42;
  --ars-wave: 0.95;
  --ars-speed: 1.2s;
}
</style>
