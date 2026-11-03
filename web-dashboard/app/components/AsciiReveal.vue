<script setup lang="ts">
import { createAsciiReveal, SKELETON_CSS } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'
import { snapshotAsciiChart } from '~/utils/asciiChart'

// Reveal ringkasan dibuat singkat agar data cepat terbaca setelah refresh.
const props = withDefaults(defineProps<{
  loading: boolean
  origin?: 'center' | 'top-left' | 'pointer' | { x: number, y: number }
  duration?: 'fast' | 'cinematic'
  speed?: number
}>(), {
  duration: 'cinematic',
  speed: 1.2
})
const root = ref<HTMLElement | null>(null)
// Kartu tetap kosong saat menunggu data; frame sudut digambar oleh efek surface.
const phase = ref(props.loading ? 'outline' : 'idle')
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()
let hasSkeleton = props.loading
useHead({ style: [{ key: 'ascii-skeleton', innerHTML: SKELETON_CSS }] })

// Teks, foto, dan grafik masuk bareng lewat satu efek ASCII.
watch([() => props.loading, reducedMotion, asciiAnimations, root], async ([loading, motion, animations, element], _previous, onCleanup) => {
  let cancelled = false
  const effect: { controller?: AsciiRevealController } = {}
  const snapshots: { remove: () => void }[] = []
  const chartAreas = element ? Array.from(element.querySelectorAll<HTMLElement>('[data-ascii-chart]')) : []
  const visibility = chartAreas.map(chart => chart.style.visibility)
  const showChartAreas = () => chartAreas.forEach((chart, index) => {
    chart.style.visibility = visibility[index] ?? ''
  })
  const removeSnapshots = () => snapshots.splice(0).forEach(snapshot => snapshot.remove())
  onCleanup(() => {
    cancelled = true
    effect.controller?.destroy()
    removeSnapshots()
    showChartAreas()
  })
  if (loading) {
    hasSkeleton = true
    phase.value = 'outline'
    return
  }
  if (!element) return
  const shouldReveal = hasSkeleton
  hasSkeleton = false
  if (motion === 'reduce' || !animations) {
    phase.value = 'idle'
    return
  }
  if (!shouldReveal) return

  chartAreas.forEach((chart) => {
    chart.style.visibility = 'hidden'
  })
  await nextTick()
  for (let frame = 0; frame < 6; frame++) {
    await new Promise(requestAnimationFrame)
    if (cancelled || chartAreas.every(area => area.querySelector('svg'))) break
  }
  if (cancelled) return

  await Promise.all(chartAreas.flatMap(area =>
    Array.from(area.querySelectorAll('svg')).map(async (svg) => {
      try {
        const snapshot = await snapshotAsciiChart(svg)
        if (!snapshot) return
        if (cancelled) snapshot.remove()
        else snapshots.push(snapshot)
      } catch {
        // Grafik asli tetap dipakai kalau snapshot gagal dibuat.
      }
    })
  ))
  if (cancelled) return
  showChartAreas()
  phase.value = 'revealing'
  await nextTick()

  // Overlay harus di luar kartu agar sudut dan titik tetap terlihat saat kartunya transparan.
  const surface = element.closest<HTMLElement>('[data-dashboard-card]')
  const controller = createAsciiReveal(surface?.parentElement ?? element, {
    preset: 'decode',
    surfaces: '[data-dashboard-card]',
    duration: props.duration,
    speed: props.speed,
    media: 'img, canvas[data-ascii-chart-snapshot]',
    lazy: false,
    // Caller bisa memilih posisi cursor sebagai titik awal gelombang reveal.
    origin: props.origin ?? 'center'
  })
  effect.controller = controller
  try {
    await controller.reveal({ within: surface ?? element })
  } catch {
    // Data tetap harus tampil kalau efek ASCII gagal.
  } finally {
    controller.destroy()
    removeSnapshots()
    if (!cancelled) phase.value = 'idle'
  }
}, { flush: 'post' })
</script>

<template>
  <div ref="root" class="min-w-0" :data-ascii-phase="phase">
    <slot v-if="loading" name="skeleton" />
    <slot v-else />
  </div>
</template>
