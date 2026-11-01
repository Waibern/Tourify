<script setup lang="ts">
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'
import type { DashboardActivityEvent } from '~/types/tourify'

const props = withDefaults(defineProps<{ activity: DashboardActivityEvent, paused?: boolean, test?: boolean }>(), { paused: false, test: false })
const emit = defineEmits<{ dismiss: [] }>()
const root = ref<HTMLElement | null>(null)
const surface = ref<HTMLElement | null>(null)
const phase = ref<'waiting' | 'revealing' | 'idle' | 'unrevealing' | 'unrevealing-surface' | 'hidden'>('waiting')
const closing = ref(false)
const reducedMotion = usePreferredReducedMotion()
const isOrder = computed(() => props.activity.type === 'order')
// Akun baru membuka pelanggan yang sama, bukan daftar umum tanpa konteks.
const target = computed(() => isOrder.value ? `/orders?ticket=${props.activity.entity_id}` : `/customers?customer=${encodeURIComponent(props.activity.entity_id)}`)
const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 })
let controller: AsciiRevealController | undefined
let disposed = false

// Simpan kontrol terakhir di luar area toast, termasuk sebelum pengguna menekan F8.
let previousFocus: HTMLElement | null = null
function rememberFocus(target: EventTarget | null) {
  if (!(target instanceof HTMLElement) || target === document.body) return
  const host = surface.value?.closest<HTMLElement>('.dashboard-activity-toast')
  if (!host?.parentElement?.contains(target)) previousFocus = target
}
useEventListener('focusin', event => rememberFocus(event.target))

// Fokus ke kartu berikutnya atau kembali bekerja, tanpa merebut fokus yang sudah dipindahkan.
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

// Tiga detik dihitung saat isi sudah terbaca penuh; hover dan fokus menyimpan sisa waktunya.
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

// Sudut dan permukaan memakai surface reveal yang sama dengan kartu Pengunjung masuk.
const { start: startEntrance, stop: stopEntrance } = useTimeoutFn(async () => {
  if (!root.value || !surface.value || disposed || closing.value) return
  if (reducedMotion.value === 'reduce') {
    // Preferensi bisa berubah sebelum entrance dimulai; waktu baca tetap harus berjalan.
    phase.value = 'idle'
    resumeLifetime()
    return
  }
  let effect: AsciiRevealController | undefined
  try {
    // Buka visibility dalam microtask yang sama sebelum library mengukur teks.
    phase.value = 'revealing'
    await nextTick()
    const card = surface.value
    const host = card?.closest<HTMLElement>('.dashboard-activity-toast') ?? card?.parentElement
    if (disposed || closing.value || !card || !host) return
    // Reveal lebih pelan supaya sudut dan perubahan ASCII kebaca sebelum kartu utuh.
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

// Balikkan surface reveal: isi dan kartu menghilang dulu, lalu bingkai sudut paling akhir.
async function dismiss() {
  if (closing.value || disposed) return
  // Catat sebelum tombol dinonaktifkan dan browser melepaskan fokusnya.
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
    if (content && card && overlayHost && phase.value !== 'waiting' && reducedMotion.value !== 'reduce') {
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
      // Habiskan glyph isi lebih dulu agar tidak tersisa setelah bingkai menghilang.
      await effect.hide({ within: content })
      if (disposed) return
      phase.value = 'unrevealing-surface'
      await nextTick()
      if (disposed) return
      // Isi yang sudah tersembunyi dilewati; surface membalik urutan kartu lalu sudutnya.
      await effect.hide({ within: card })
    }
  } catch {
    // Kartu tetap bisa ditutup meski browser gagal menjalankan efeknya.
  } finally {
    if (!disposed) {
      // Tahan seluruh kartu tetap tersembunyi setelah sudut selesai; reduce motion memakai fade biasa.
      phase.value = reducedMotion.value === 'reduce' ? 'idle' : 'hidden'
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
  if (reducedMotion.value === 'reduce') phase.value = 'idle'
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
  <!-- Permukaan terpisah dari host overlay agar bingkai sudut tidak ikut tersembunyi. -->
  <div ref="surface" data-notification-card class="rounded-xl border border-default bg-default px-4 py-3">
    <!-- Isi tetap terukur untuk ASCII; jarak antar bagian tetap ringkas. -->
    <div
      ref="root"
      :data-activity-reveal="phase"
      :data-ascii="phase === 'unrevealing-surface' ? 'skip' : undefined"
      class="relative min-w-0 pr-7"
    >
      <!-- Penutup sendiri menjaga toast tetap terpasang sampai unreveal selesai. -->
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
        <UIcon :name="isOrder ? 'i-lucide-receipt-text' : 'i-lucide-user-round-plus'" data-ascii="fade" class="size-5 shrink-0 text-primary" />
        <span>{{ test ? (isOrder ? 'Notifikasi uji' : 'Uji akun baru') : isOrder ? 'Pesanan baru' : 'Akun baru' }}</span>
      </div>
      <p class="text-sm leading-5 break-words text-muted">
        <!-- Uji akun memakai data yang sudah ada, jadi jangan mengumumkannya sebagai pendaftaran sungguhan. -->
        <span v-if="test && !isOrder">Akun contoh: </span>
        <span class="text-highlighted">{{ activity.username }}</span>
        {{ activity.type === 'order' ? ` memesan ${activity.quantity} tiket di ${activity.wahana_name}.` : test ? '' : ' baru saja mendaftar.' }}
      </p>
      <div class="mt-1 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm">
        <span v-if="activity.type === 'order'" class="font-medium text-highlighted tabular-nums">
          {{ rupiah.format(activity.total_price) }}
        </span>
        <!-- Area sentuh 44px memakai margin negatif agar tinggi kartu tetap ringkas. -->
        <UButton
          v-if="test && isOrder"
          label="Tutup uji"
          color="primary"
          variant="link"
          :disabled="closing"
          class="-my-2 -mr-1 min-h-11 px-1 py-0 text-sm"
          @click="dismiss"
        />
        <NuxtLink
          v-else
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
