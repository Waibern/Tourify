<script setup lang="ts">
import type QrScanner from 'qr-scanner'
import type { CheckinApiResponse, CheckinTicket } from '~/types/tourify'

const video = ref<HTMLVideoElement | null>(null)
const resultPanel = ref<HTMLElement | null>(null)
const api = useTourifyApi()
const phase = ref<'idle' | 'starting' | 'scanning' | 'checking' | 'result' | 'camera-error'>('idle')
const resultKind = ref<'success' | 'error'>('success')
const resultMessage = ref('')
const cameraMessage = ref('')
const scannedTicket = ref<CheckinTicket | null>(null)
let scanner: QrScanner | null = null

// Tombol lanjut langsung siap dipakai dengan keyboard setelah hasil tampil.
watch(phase, async (value) => {
  if (value === 'result') {
    await nextTick()
    resultPanel.value?.querySelector('button')?.focus({ preventScroll: true })
  }
})

// Kamera dibuat setelah halaman tampil supaya akses browser tidak dipanggil saat SSR.
onMounted(async () => {
  if (!video.value) {
    return
  }

  try {
    const { default: QrScannerBrowser } = await import('qr-scanner')
    scanner = new QrScannerBrowser(
      video.value,
      result => handleQr(result.data),
      {
        preferredCamera: 'environment',
        highlightScanRegion: false,
        highlightCodeOutline: false,
        returnDetailedScanResult: true
      }
    )
  } catch {
    phase.value = 'camera-error'
    cameraMessage.value = 'Pemindai QR gagal disiapkan di browser ini.'
  }
})

// Stream kamera dilepas saat halaman ditinggalkan.
onBeforeUnmount(() => {
  scanner?.destroy()
})

function ticketIdFromQr(value: string): string | null {
  const parts = value.split('|')
  if (parts.length !== 6 || parts[0] !== 'TOURIFY' || !/^[1-9]\d{0,19}$/.test(parts[1] ?? '')) {
    return null
  }

  return parts[1] ?? null
}

// Satu QR menghitung satu orang; server yang menentukan validitas dan sisa tiket.
async function handleQr(value: string) {
  if (phase.value !== 'scanning') {
    return
  }

  scanner?.stop()
  phase.value = 'checking'
  resultMessage.value = ''
  scannedTicket.value = null

  const ticketId = ticketIdFromQr(value)
  if (!ticketId) {
    resultKind.value = 'error'
    resultMessage.value = 'QR ini bukan tiket Tourify yang valid.'
    phase.value = 'result'
    return
  }

  try {
    const response = await api.post<CheckinApiResponse>(
      'dashboard_checkin.php',
      { ticket_id: ticketId }
    )
    if (!response.ticket) {
      throw new Error(response.message || 'Respons check-in belum lengkap.')
    }

    scannedTicket.value = response.ticket
    resultKind.value = 'success'
    resultMessage.value = response.message || 'Check-in berhasil.'
  } catch (error) {
    resultKind.value = 'error'
    resultMessage.value = getTourifyErrorMessage(error, 'Check-in gagal. Periksa koneksi lalu coba lagi.')
  }

  phase.value = 'result'
}

// Memulai kamera hanya setelah petugas menekan tombol dan memberi izin browser.
async function startScanner() {
  if (!scanner) {
    phase.value = 'camera-error'
    cameraMessage.value = 'Kamera belum siap. Muat ulang halaman lalu coba lagi.'
    return
  }

  cameraMessage.value = ''
  phase.value = 'starting'
  try {
    await scanner.start()
    phase.value = 'scanning'
  } catch (error) {
    phase.value = 'camera-error'
    const name = typeof error === 'object' && error !== null && 'name' in error
      ? String(error.name)
      : ''
    cameraMessage.value = name === 'NotAllowedError'
      ? 'Izin kamera ditolak. Izinkan kamera dari pengaturan browser.'
      : name === 'NotFoundError'
        ? 'Kamera tidak ditemukan di perangkat ini.'
        : 'Kamera tidak bisa dibuka. Pastikan halaman memakai HTTPS atau localhost.'
  }
}

function stopScanner() {
  scanner?.stop()
  phase.value = 'idle'
}

async function scanNext() {
  scannedTicket.value = null
  resultMessage.value = ''
  await startScanner()
}
</script>

<template>
  <UDashboardPanel id="scanner">
    <template #header>
      <UDashboardNavbar title="Pemindai QR">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <!-- Kamera dan hasil berdampingan supaya petugas tidak perlu menggulir setelah scan. -->
      <div class="mx-auto w-full max-w-6xl space-y-6">
        <div>
          <h1 class="text-xl font-semibold text-highlighted">
            Check-in pengunjung
          </h1>
          <p class="mt-1 text-sm text-muted">
            Arahkan QR tiket ke kamera. Setiap scan mencatat satu orang masuk.
          </p>
        </div>

        <div class="grid items-start gap-5 lg:grid-cols-[minmax(0,1.6fr)_minmax(300px,1fr)]">
          <!-- Di HP, hasil menggantikan kamera supaya langsung terlihat setelah scan. -->
          <UCard :ui="{ body: 'p-0 sm:p-0' }" :class="{ 'hidden lg:block': phase === 'result' }">
            <!-- Video tetap terpasang; lapisan status menjelaskan kamera tanpa menampilkan layar hitam kosong. -->
            <div class="relative aspect-square overflow-hidden bg-zinc-950 sm:aspect-[4/3]">
              <video
                ref="video"
                muted
                playsinline
                class="h-full w-full object-cover"
                :class="{ 'opacity-30': phase !== 'scanning' }"
                aria-label="Tampilan kamera untuk scan QR tiket"
              />

              <div v-if="phase === 'scanning'" class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-6">
                <div class="size-48 rounded-3xl border-2 border-white/90 shadow-[0_0_0_999px_rgba(0,0,0,0.35)] sm:size-60" />
                <span class="relative rounded-full bg-black/60 px-4 py-2 text-sm text-white">Posisikan QR di dalam bingkai</span>
              </div>

              <div v-else class="absolute inset-0 flex flex-col items-center justify-center p-6 text-center text-white">
                <UIcon
                  :name="phase === 'checking' || phase === 'starting' ? 'i-lucide-loader-circle' : phase === 'result' ? 'i-lucide-scan-line' : 'i-lucide-camera'"
                  class="mb-5 size-10 text-white/80"
                  :class="{ 'animate-spin': phase === 'checking' || phase === 'starting' }"
                />
                <h2 class="text-lg font-semibold">
                  {{ phase === 'checking' ? 'Memeriksa tiket…' : phase === 'starting' ? 'Membuka kamera…' : phase === 'result' ? 'Pemindaian dijeda' : phase === 'camera-error' ? 'Kamera belum bisa dibuka' : 'Siap menerima pengunjung?' }}
                </h2>
                <p class="mt-2 max-w-xs text-sm text-white/65">
                  {{ phase === 'checking' ? 'Tunggu hasil sebelum memindai tiket berikutnya.' : phase === 'result' ? 'Periksa hasil check-in, lalu lanjutkan ke pengunjung berikutnya.' : phase === 'camera-error' ? cameraMessage : 'Aktifkan kamera dan izinkan akses saat diminta browser.' }}
                </p>
                <UButton
                  v-if="phase === 'idle' || phase === 'camera-error'"
                  class="mt-6"
                  size="lg"
                  :label="phase === 'camera-error' ? 'Coba buka kamera' : 'Aktifkan kamera'"
                  icon="i-lucide-camera"
                  @click="startScanner"
                />
              </div>

              <div class="absolute inset-x-0 top-0 flex items-center justify-between gap-3 p-4">
                <UBadge :color="phase === 'scanning' ? 'success' : 'neutral'" variant="solid">
                  {{ phase === 'scanning' ? 'Kamera aktif' : phase === 'checking' ? 'Memeriksa' : phase === 'starting' ? 'Menghubungkan' : 'Kamera dijeda' }}
                </UBadge>
                <UButton
                  v-if="phase === 'scanning'"
                  label="Hentikan"
                  icon="i-lucide-camera-off"
                  color="neutral"
                  variant="solid"
                  size="sm"
                  @click="stopScanner"
                />
              </div>
            </div>
            <div class="flex items-start gap-2.5 p-4 text-sm text-muted">
              <UIcon name="i-lucide-info" class="mt-0.5 size-4 shrink-0" />
              <p>Satu pesanan bisa untuk beberapa orang. Pindai ulang QR yang sama untuk setiap orang yang masuk.</p>
            </div>
          </UCard>

          <!-- Hasil tetap di tempat yang sama; warna dan teks membedakan berhasil, gagal, dan menunggu. -->
          <UCard>
            <div
              ref="resultPanel"
              role="status"
              aria-live="polite"
              aria-atomic="true"
              class="space-y-5"
            >
              <h2 class="text-sm font-semibold text-highlighted">
                Hasil pemeriksaan
              </h2>

              <template v-if="phase === 'result'">
                <div class="flex items-start gap-3">
                  <div class="flex size-12 shrink-0 items-center justify-center rounded-full" :class="resultKind === 'success' ? 'bg-success/10 text-success' : 'bg-error/10 text-error'">
                    <UIcon :name="resultKind === 'success' ? 'i-lucide-check' : 'i-lucide-x'" class="size-6" />
                  </div>
                  <div>
                    <h3 class="text-lg font-semibold text-highlighted">
                      {{ resultKind === 'success' ? 'Pengunjung boleh masuk' : 'Check-in gagal' }}
                    </h3>
                    <p class="mt-1 text-sm text-muted">
                      {{ resultKind === 'success' ? 'Satu orang berhasil dicatat masuk.' : resultMessage }}
                    </p>
                  </div>
                </div>

                <template v-if="scannedTicket">
                  <dl class="space-y-4 border-y border-default py-4 text-sm">
                    <div>
                      <dt class="text-muted">
                        Wahana
                      </dt>
                      <dd class="mt-1 font-medium text-highlighted">
                        {{ scannedTicket.wahana_name }}
                      </dd>
                    </div>
                    <div class="flex justify-between gap-3">
                      <dt class="text-muted">
                        Nomor tiket
                      </dt>
                      <dd class="font-mono text-highlighted">
                        #{{ scannedTicket.id }}
                      </dd>
                    </div>
                    <div class="flex justify-between gap-3">
                      <dt class="text-muted">
                        Tanggal kunjungan
                      </dt>
                      <dd class="text-highlighted">
                        {{ scannedTicket.visit_date }}
                      </dd>
                    </div>
                  </dl>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <p class="text-sm text-muted">
                        Sudah masuk
                      </p>
                      <p class="mt-1 text-2xl font-semibold tabular-nums text-highlighted">
                        {{ scannedTicket.checked_in_quantity }} <span class="text-sm font-normal text-muted">/ {{ scannedTicket.quantity }}</span>
                      </p>
                    </div>
                    <div>
                      <p class="text-sm text-muted">
                        Belum masuk
                      </p>
                      <p class="mt-1 text-2xl font-semibold tabular-nums text-highlighted">
                        {{ scannedTicket.remaining_quantity }} <span class="text-sm font-normal text-muted">orang</span>
                      </p>
                    </div>
                  </div>
                  <p v-if="scannedTicket.remaining_quantity === 0" class="text-sm text-muted">
                    Semua pengunjung dalam pesanan ini sudah masuk.
                  </p>
                </template>

                <UButton
                  label="Scan berikutnya"
                  icon="i-lucide-scan-line"
                  size="lg"
                  block
                  @click="scanNext"
                />
                <p class="text-xs text-muted">
                  Lanjutkan setelah pengunjung melewati gate agar tidak terhitung dua kali.
                </p>
              </template>

              <div v-else class="flex min-h-48 flex-col items-center justify-center py-6 text-center">
                <UIcon :name="phase === 'checking' ? 'i-lucide-loader-circle' : 'i-lucide-ticket'" class="mb-4 size-8 text-dimmed" :class="{ 'animate-spin': phase === 'checking' }" />
                <h3 class="font-medium text-highlighted">
                  {{ phase === 'checking' ? 'Mencocokkan tiket' : 'Menunggu tiket' }}
                </h3>
                <p class="mt-2 max-w-xs text-sm text-muted">
                  {{ phase === 'checking' ? 'Hasil validasi akan tampil di sini.' : 'Hasil check-in dan sisa pengunjung akan tampil di sini setelah QR terbaca.' }}
                </p>
              </div>
            </div>
          </UCard>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
