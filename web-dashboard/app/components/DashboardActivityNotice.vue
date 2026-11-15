<script setup lang="ts">
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'
import type { DashboardActivityEvent } from '~/types/tourify'

const props = withDefaults(defineProps<{
  activity?: DashboardActivityEvent
  title?: string
  description?: string
  icon?: string
  paused?: boolean
  test?: boolean
}>(), { paused: false, test: false })
const emit = defineEmits<{ dismiss: [] }>()
const root = ref<HTMLElement | null>(null)
const surface = ref<HTMLElement | null>(null)
const phase = ref<'waiting' | 'revealing' | 'idle' | 'unrevealing' | 'unrevealing-surface' | 'hidden'>('waiting')
const closing = ref(false)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()
// Kartu ini dipakai untuk notifikasi aktivitas dan pesan sukses.
const isOrder = computed(() => props.activity?.type === 'order')
// Pilih judul khusus, judul tes, atau judul sesuai aktivitas.
const title = computed(() => {
  if (props.title !== undefined && props.title !== null) {
    return props.title
  }

  if (props.test) {
    return isOrder.value ? 'Notifikasi uji' : 'Uji akun baru'
  }

  return isOrder.value ? 'Pesanan baru' : 'Akun baru'
})
const description = computed(() => {
  if (props.description !== undefined) return props.description
  if (!props.activity) return ''
  if (props.activity.type === 'order') return `${props.activity.username} memesan ${props.activity.quantity} tiket di ${props.activity.wahana_name}.`
  return props.test ? `Akun contoh: ${props.activity.username}` : `${props.activity.username} baru saja mendaftar.`
})
// Pilih ikon sesuai isi kartu.
const icon = computed(() => {
  if (props.icon !== undefined && props.icon !== null) {
    return props.icon
  }

  if (isOrder.value) {
    return 'i-lucide-receipt-text'
  }

  if (props.activity) {
    return 'i-lucide-user-round-plus'
  }

  return 'i-lucide-circle-check'
})
// Notifikasi akun baru membuka daftar yang sudah difilter ke pelanggan itu.
const target = computed(() => {
  if (!props.activity) return undefined
  return isOrder.value ? `/orders?ticket=${props.activity.entity_id}` : `/customers?customer=${encodeURIComponent(props.activity.entity_id)}`
})
const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 })
let controller: AsciiRevealController | undefined
let disposed = false

// Simpan tempat yang terakhir dipilih, termasuk sebelum tombol F8 ditekan.
let previousFocus: HTMLElement | null = null
function rememberFocus(target: EventTarget | null) {
  if (!(target instanceof HTMLElement) || target === document.body) return
  const host = surface.value?.closest<HTMLElement>('.dashboard-activity-toast')
  if (!host?.parentElement?.contains(target)) previousFocus = target
}
useEventListener('focusin', event => rememberFocus(event.target))

// Setelah notifikasi ditutup, kembalikan fokus kalau pengguna belum memilih tempat lain.
function restoreFocus(host: HTMLElement | null, hadFocus: boolean) {
  const active = document.activeElement
  const viewport = host?.parentElement
  if (!hadFocus || (active !== document.body && active !== viewport && !host?.contains(active))) return
  const nextNotice = Array.from(viewport?.querySelectorAll<HTMLElement>('.dashboard-activity-toast[data-state="open"]') ?? [])
    .find(item => item !== host && item.querySelector('[data-activity-reveal="idle"], [data-activity-reveal="revealing"]'))
  const origin = previousFocus?.isConnected && !previousFocus.matches(':disabled') ? previousFocus : null
  const destination = nextNotice ?? origin ?? document.querySelector<HTMLAnchorElement>('a[href="/"]')
  destination?.focus({ preventScroll: true })
}

// Hitung tiga detik setelah kartu terbuka; jeda hitungan saat kartu disorot atau dipilih.
const remaining = ref(3000)
let startedAt: number | null = null
const { start: startLifetime, stop: stopLifetime } = useTimeoutFn(() => {
  void dismiss()
}, remaining, { immediate: false })
function pauseLifetime() {
  if (startedAt === null) return
  remaining.value = Math.max(0, remaining.value - (Date.now() - startedAt))
  startedAt = null
  stopLifetime()
}
function resumeLifetime() {
  if (phase.value !== 'idle' || props.paused || closing.value || disposed || startedAt !== null) return
  startedAt = Date.now()
  startLifetime()
}
watch(() => props.paused, paused => paused ? pauseLifetime() : resumeLifetime())

// Animasi kartu mengikuti cara yang sama dengan kartu Pengunjung masuk.
const { start: startEntrance, stop: stopEntrance } = useTimeoutFn(async () => {
  if (!root.value || !surface.value || disposed || closing.value) return
  if (reducedMotion.value === 'reduce' || !asciiAnimations.value) {
    // Kalau animasi dimatikan sebelum kartu muncul, timer-nya tetap berjalan.
    phase.value = 'idle'
    resumeLifetime()
    return
  }
  let effect: AsciiRevealController | undefined
  try {
    // Tampilkan isi sebelum pustaka menghitung ukuran teks.
    phase.value = 'revealing'
    await nextTick()
    const card = surface.value
    const host = card?.closest<HTMLElement>('.dashboard-activity-toast') ?? card?.parentElement
    if (disposed || closing.value || !card || !host) return
    // Perlambat animasi supaya isi kartu sempat terbaca sebelum tampil penuh.
    effect = createAsciiReveal(host, {
      preset: 'decode',
      surfaces: '[data-notification-card]',
      duration: 'fast',
      speed: 0.65,
      origin: 'center',
      lazy: false
    })
    controller = effect
    await effect.reveal({ within: card })
  } catch {
    // Isi tetap terlihat kalau browser tidak bisa menjalankan efeknya.
  } finally {
    if (controller === effect) {
      effect?.destroy()
      controller = undefined
    }
    if (!disposed && !closing.value) {
      phase.value = 'idle'
      resumeLifetime()
    }
  }
}, 0, { immediate: false })

// Kalau animasi dimatikan, hentikan efeknya dan langsung tampilkan kartu.
watch([asciiAnimations, reducedMotion], ([animations, motion]) => {
  if (animations && motion !== 'reduce') {
    if (phase.value === 'waiting') startEntrance()
    return
  }

  stopEntrance()
  controller?.destroy()
  controller = undefined
  if (!closing.value) {
    phase.value = 'idle'
    resumeLifetime()
  }
})

// Saat ditutup, sembunyikan isi dan kartu dulu, lalu hilangkan bingkainya.
async function dismiss() {
  if (closing.value || disposed) return
  // Simpan fokus sebelum tombol ditutup dan browser melepasnya.
  const host = surface.value?.closest<HTMLElement>('.dashboard-activity-toast') ?? null
  const hadFocus = !!host?.contains(document.activeElement)
  closing.value = true
  stopEntrance()
  pauseLifetime()
  controller?.destroy()
  controller = undefined
  try {
    const content = root.value
    const card = surface.value
    const overlayHost = host ?? card?.parentElement
    if (content && card && overlayHost && phase.value !== 'waiting' && reducedMotion.value !== 'reduce' && asciiAnimations.value) {
      phase.value = 'unrevealing'
      const effect = createAsciiReveal(overlayHost, {
        preset: 'decode',
        surfaces: '[data-notification-card]',
        duration: 'fast',
        speed: 1.2,
        origin: 'center',
        lazy: false
      })
      controller = effect
      // Sembunyikan isi dulu supaya teks tidak tertinggal setelah bingkai hilang.
      await effect.hide({ within: content })
      if (disposed) return
      phase.value = 'unrevealing-surface'
      await nextTick()
      if (disposed) return
      // Setelah isi hilang, tutup kartu dan bingkainya.
      await effect.hide({ within: card })
    }
  } catch {
    // Kartu tetap bisa ditutup meski browser gagal menjalankan efeknya.
  } finally {
    if (!disposed) {
      // Setelah bingkai hilang, biarkan kartu tersembunyi; opsi kurangi gerakan memakai fade.
      phase.value = reducedMotion.value === 'reduce' || !asciiAnimations.value ? 'idle' : 'hidden'
      await nextTick()
      controller?.destroy()
      controller = undefined
      if (!disposed) {
        restoreFocus(host, hadFocus)
        emit('dismiss')
      }
    }
  }
}
defineExpose({ dismiss })

onMounted(() => {
  rememberFocus(document.activeElement)
  if (reducedMotion.value === 'reduce' || !asciiAnimations.value) phase.value = 'idle'
  else startEntrance()
  resumeLifetime()
})
onBeforeUnmount(() => {
  disposed = true
  stopEntrance()
  stopLifetime()
  controller?.destroy()
  controller = undefined
})
</script>

<template>
  <!-- Pisahkan kartu dari latar notifikasi supaya bingkainya tetap bisa dianimasikan. -->
  <div ref="surface" data-notification-card class="rounded-xl border border-default bg-default px-4 py-3">
    <!-- Isi tetap bisa diukur animasi tanpa menambah jarak kartu. -->
    <div
      ref="root"
      :data-activity-reveal="phase"
      :data-ascii="phase === 'unrevealing-surface' ? 'skip' : undefined"
      class="relative min-w-0 pr-7"
    >
      <!-- Notifikasi tetap ada sampai animasi penutup selesai. -->
      <UButton
        icon="i-lucide-x"
        color="neutral"
        variant="link"
        aria-label="Tutup notifikasi"
        data-ascii="fade"
        :disabled="closing"
        class="absolute -top-2 -right-2 size-11 justify-center p-2.5"
        @click="dismiss"
      />
      <div class="mb-1 flex items-center gap-2 text-base font-medium text-highlighted">
        <UIcon :name="icon" data-ascii="fade" class="size-5 shrink-0 text-primary" />
        <span>{{ title }}</span>
      </div>
      <p class="text-sm leading-5 break-words text-muted">
        {{ description }}
      </p>
      <div class="mt-1 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm">
        <span v-if="activity?.type === 'order'" class="font-medium text-highlighted tabular-nums">
          {{ rupiah.format(activity.total_price) }}
        </span>
        <!-- Tombol mudah ditekan, tapi marginnya menjaga kartu tetap ringkas. -->
        <UButton
          v-if="activity && test && isOrder"
          label="Tutup uji"
          color="primary"
          variant="link"
          :disabled="closing"
          class="-my-2 -mr-1 min-h-11 px-1 py-0 text-sm"
          @click="dismiss"
        />
        <NuxtLink
          v-else-if="activity"
          :to="target"
          class="-my-2 -mr-1 inline-flex min-h-11 items-center gap-1 rounded px-1 text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
          @click="dismiss"
        >
          {{ isOrder ? 'Lihat pesanan' : 'Lihat pelanggan' }}
          <UIcon name="i-lucide-arrow-up-right" class="size-4" />
        </NuxtLink>
      </div>
    </div>
  </div>
</template>
