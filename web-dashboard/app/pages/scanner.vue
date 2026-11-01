<script setup lang="ts">
import type QrScanner from 'qr-scanner'
import type { CheckinApiResponse, CheckinTicket } from '~/types/tourify'

const video = ref<HTMLVideoElement | null>(null)
const resultPanel = ref<HTMLElement | null>(null)
const api = useTourifyApi()
const { user } = useDashboardAuth()
const phase = ref<'preparing' | 'idle' | 'starting' | 'scanning' | 'checking' | 'result' | 'camera-error'>('preparing')
const resultKind = ref<'success' | 'error'>('success')
const resultMessage = ref('')
const cameraMessage = ref('')
const scannedTicket = ref<CheckinTicket | null>(null)
const scannedTicketId = ref<string | null>(null)
const uncertainResult = ref(false)
const statusChecked = ref(false)
const cameraDevices = ref<QrScanner.Camera[]>([])
const selectedCameraId = ref('')
let scanner: QrScanner | null = null
let listCameraDevices: (() => Promise<QrScanner.Camera[]>) | null = null
let disposed = false
let cameraStartRequest = 0

const cameraOptions = computed(() => cameraDevices.value.map(camera => ({
  label: camera.label,
  value: camera.id
})))

const cameraStatusLabel = computed(() => {
  if (phase.value === 'preparing') return 'Menyiapkan pemindai'
  if (phase.value === 'starting') return 'Membuka kamera'
  if (phase.value === 'scanning') return 'Kamera aktif'
  if (phase.value === 'checking') return 'Memeriksa tiket'
  if (phase.value === 'camera-error') return 'Kamera bermasalah'
  if (phase.value === 'result') return 'Pemindaian dijeda'
  return 'Kamera siap'
})

const cameraStatusColor = computed(() => {
  if (phase.value === 'scanning') return 'success'
  if (phase.value === 'checking') return 'warning'
  if (phase.value === 'camera-error') return 'error'
  return 'neutral'
})

const cameraOverlayTitle = computed(() => {
  if (phase.value === 'preparing') return 'Menyiapkan pemindai'
  if (phase.value === 'starting') return 'Membuka kamera…'
  if (phase.value === 'checking') return 'Memeriksa tiket…'
  if (phase.value === 'result') return 'Pemindaian dijeda'
  if (phase.value === 'camera-error') return 'Kamera belum bisa dibuka'
  return 'Siap menerima pengunjung?'
})

const cameraOverlayDescription = computed(() => {
  if (phase.value === 'preparing') return 'Pemindai sedang dimuat. Tombol kamera akan tersedia sebentar lagi.'
  if (phase.value === 'starting') return 'Izinkan akses kamera jika browser memintanya.'
  if (phase.value === 'checking') return 'Tunggu hasil server sebelum memindai tiket berikutnya.'
  if (phase.value === 'result') return 'Periksa hasil check-in sebelum lanjut ke pengunjung berikutnya.'
  if (phase.value === 'camera-error') return cameraMessage.value
  if (cameraMessage.value) return cameraMessage.value
  return 'Aktifkan kamera, lalu pilih kamera yang sesuai jika tersedia.'
})

// Tombol lanjut langsung siap dipakai dengan keyboard setelah hasil tampil.
watch(phase, async (value) => {
  if (value === 'result') {
    await nextTick()
    const action = resultPanel.value?.querySelector('button')
    if (action) action.focus({ preventScroll: true })
    else resultPanel.value?.focus({ preventScroll: true })
  }
})

// Pemindai hanya dibuat di browser dan bisa dimuat ulang dari tombol kamera.
async function initializeScanner() {
  if (disposed || scanner) return
  if (!video.value) {
    phase.value = 'camera-error'
    cameraMessage.value = 'Tampilan kamera tidak tersedia. Muat ulang halaman lalu coba lagi.'
    return
  }

  phase.value = 'preparing'
  cameraMessage.value = ''
  try {
    const { default: QrScannerBrowser } = await import('qr-scanner')
    if (disposed) return
    listCameraDevices = () => QrScannerBrowser.listCameras()
    scanner = new QrScannerBrowser(
      video.value,
      result => handleQr(result.data),
      {
        preferredCamera: selectedCameraId.value || 'environment',
        highlightScanRegion: false,
        highlightCodeOutline: false,
        returnDetailedScanResult: true
      }
    )
    phase.value = 'idle'
  } catch {
    if (disposed) return
    phase.value = 'camera-error'
    cameraMessage.value = 'Pemindai QR gagal disiapkan di browser ini.'
  }
}

// Nama kamera biasanya baru tersedia setelah browser memberi izin ke stream.
async function refreshCameraDevices(request: number) {
  if (!listCameraDevices) return

  try {
    const devices = await listCameraDevices()
    if (disposed || request !== cameraStartRequest) return
    cameraDevices.value = devices

    const stream = video.value?.srcObject
    const activeCameraId = stream instanceof MediaStream
      ? stream.getVideoTracks()[0]?.getSettings().deviceId
      : undefined
    if (activeCameraId && devices.some(camera => camera.id === activeCameraId)) {
      selectedCameraId.value = activeCameraId
    }
  } catch {
    // Pemindaian tetap jalan kalau browser gagal mencantumkan kamera.
  }
}

// Kamera aktif langsung beralih; saat jeda, pilihan disimpan untuk scan berikutnya.
async function selectCamera(cameraId: string) {
  if (!scanner || !cameraId || cameraId === selectedCameraId.value) return

  const previousCameraId = selectedCameraId.value
  const resumeScanning = phase.value === 'scanning'
  const request = ++cameraStartRequest
  if (resumeScanning) phase.value = 'starting'

  try {
    await scanner.setCamera(cameraId)
    if (disposed || request !== cameraStartRequest) return
    selectedCameraId.value = cameraId
    if (resumeScanning) {
      phase.value = 'scanning'
      void refreshCameraDevices(request)
    }
  } catch {
    if (disposed || request !== cameraStartRequest) return
    if (previousCameraId) {
      try {
        await scanner.setCamera(previousCameraId)
      } catch {
        // Tombol kamera tetap tersedia untuk mencoba ulang.
      }
    }
    selectedCameraId.value = previousCameraId
    if (resumeScanning) {
      phase.value = 'camera-error'
      cameraMessage.value = 'Kamera yang dipilih tidak bisa dibuka. Coba pilih kamera lain.'
    }
  }
}

// Kamera baru disiapkan setelah halaman tampil supaya akses browser tidak dipanggil saat SSR.
onMounted(() => {
  document.addEventListener('visibilitychange', handlePageVisibility)
  void initializeScanner()
})

// Kamera berhenti saat tab disembunyikan dan saat halaman ditinggalkan.
function handlePageVisibility() {
  if (document.visibilityState !== 'hidden' || !['starting', 'scanning'].includes(phase.value)) {
    return
  }

  cameraStartRequest++
  scanner?.stop()
  phase.value = 'idle'
  cameraMessage.value = 'Pemindaian dijeda karena tab tidak aktif. Aktifkan kamera lagi sebelum memindai.'
}

onBeforeUnmount(() => {
  disposed = true
  cameraStartRequest++
  document.removeEventListener('visibilitychange', handlePageVisibility)
  scanner?.destroy()
})

function ticketIdFromQr(value: string): string | null {
  const parts = value.trim().split('|')
  if (parts.length !== 6 || parts[0] !== 'TOURIFY' || !/^[1-9]\d{0,19}$/.test(parts[1] ?? '')) {
    return null
  }

  return parts[1] ?? null
}

function responseStatusCode(error: unknown): number | null {
  if (typeof error !== 'object' || error === null) return null
  if ('statusCode' in error && typeof error.statusCode === 'number' && error.statusCode > 0) return error.statusCode
  if ('status' in error && typeof error.status === 'number' && error.status > 0) return error.status
  return null
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
  scannedTicketId.value = null
  uncertainResult.value = false
  statusChecked.value = false

  const ticketId = ticketIdFromQr(value)
  if (!ticketId) {
    resultKind.value = 'error'
    resultMessage.value = 'QR ini bukan tiket Tourify yang valid.'
    phase.value = 'result'
    return
  }
  scannedTicketId.value = ticketId

  try {
    const response = await api.post<CheckinApiResponse>(
      'dashboard_checkin.php',
      { ticket_id: ticketId },
      { timeout: 15_000 }
    )
    if (!response.ticket) {
      throw new Error(response.message || 'Respons check-in belum lengkap.')
    }

    scannedTicket.value = response.ticket
    resultKind.value = 'success'
    resultMessage.value = response.message || 'Check-in berhasil.'
  } catch (error) {
    resultKind.value = 'error'
    const statusCode = responseStatusCode(error)
    uncertainResult.value = statusCode === null || statusCode >= 500
    resultMessage.value = uncertainResult.value
      ? 'Server belum mengirim konfirmasi. Check-in mungkin sudah tercatat.'
      : getTourifyErrorMessage(error, 'Check-in gagal. Periksa koneksi lalu coba lagi.')
  }

  phase.value = 'result'
}

// Memulai kamera hanya setelah petugas menekan tombol dan memberi izin browser.
async function startScanner() {
  if (!scanner) {
    await initializeScanner()
    if (!scanner || disposed) return
  }

  cameraMessage.value = ''
  phase.value = 'starting'
  const request = ++cameraStartRequest
  try {
    await scanner.start()
    if (disposed || document.visibilityState === 'hidden' || request !== cameraStartRequest) {
      scanner.stop()
      if (!disposed && phase.value === 'starting') phase.value = 'idle'
      return
    }
    phase.value = 'scanning'
    void refreshCameraDevices(request)
  } catch (error) {
    if (disposed || request !== cameraStartRequest) return
    phase.value = 'camera-error'
    const name = typeof error === 'object' && error !== null && 'name' in error
      ? String(error.name)
      : ''
    if (!window.isSecureContext) {
      cameraMessage.value = 'Kamera membutuhkan HTTPS. Gunakan alamat HTTPS di tempat kerja atau localhost saat pengembangan.'
    } else if (name === 'NotAllowedError') {
      cameraMessage.value = 'Izin kamera ditolak. Izinkan kamera dari pengaturan browser.'
    } else if (name === 'NotFoundError') {
      cameraMessage.value = 'Kamera tidak ditemukan di perangkat ini.'
    } else if (name === 'NotReadableError') {
      cameraMessage.value = 'Kamera sedang dipakai aplikasi lain. Tutup aplikasi itu lalu coba lagi.'
    } else {
      cameraMessage.value = 'Kamera tidak bisa dibuka. Pastikan halaman memakai HTTPS atau localhost.'
    }
  }
}

function stopScanner() {
  cameraStartRequest++
  scanner?.stop()
  phase.value = 'idle'
}

async function scanNext() {
  if (uncertainResult.value && !statusChecked.value) return
  scannedTicket.value = null
  scannedTicketId.value = null
  resultMessage.value = ''
  uncertainResult.value = false
  statusChecked.value = false
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
      <div class="mx-auto w-full max-w-6xl space-y-5">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 class="text-xl font-semibold text-highlighted">
              Check-in pengunjung
            </h1>
            <p class="mt-1 text-sm text-muted">
              Arahkan kamera ke QR tiket. Satu scan mencatat satu pengunjung.
            </p>
          </div>
          <div class="flex flex-wrap items-end gap-3">
            <UFormField
              v-if="cameraDevices.length > 1"
              label="Kamera"
              class="w-48"
            >
              <USelect
                :model-value="selectedCameraId"
                :items="cameraOptions"
                placeholder="Pilih kamera"
                aria-label="Pilih kamera pemindai QR"
                :disabled="['preparing', 'starting', 'checking'].includes(phase)"
                class="w-full"
                @update:model-value="selectCamera"
              />
            </UFormField>
            <UBadge
              :color="cameraStatusColor"
              variant="subtle"
              aria-live="polite"
            >
              {{ cameraStatusLabel }}
            </UBadge>
          </div>
        </div>

        <div class="grid items-stretch gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(300px,0.9fr)]">
          <!-- Di HP, kamera disembunyikan saat hasil supaya hasil check-in tetap jelas. -->
          <UCard
            class="h-full overflow-hidden"
            :ui="{ body: 'p-0 sm:p-0' }"
            :class="{ 'hidden lg:block': phase === 'result' }"
          >
            <div class="relative aspect-[4/3] overflow-hidden bg-zinc-950">
              <video
                ref="video"
                muted
                playsinline
                class="h-full w-full object-cover"
                :class="{ 'opacity-30': phase !== 'scanning' }"
                aria-label="Tampilan kamera untuk scan QR tiket"
              />

              <div
                v-if="phase === 'scanning'"
                class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-5"
              >
                <div class="size-48 rounded-3xl border-2 border-white/90 shadow-[0_0_0_999px_rgba(0,0,0,0.38)] sm:size-60" />
                <span class="rounded-full bg-black/70 px-4 py-2 text-sm text-white">
                  Posisikan QR di dalam bingkai
                </span>
              </div>

              <div
                v-else
                class="absolute inset-0 flex flex-col items-center justify-center p-6 text-center text-white"
              >
                <UIcon
                  :name="phase === 'checking' || phase === 'starting' || phase === 'preparing' ? 'i-lucide-loader-circle' : phase === 'result' ? 'i-lucide-scan-line' : 'i-lucide-camera'"
                  class="mb-4 size-10 text-white/80"
                  :class="{ 'animate-spin motion-reduce:animate-none': phase === 'checking' || phase === 'starting' || phase === 'preparing' }"
                  aria-hidden="true"
                />
                <h2 class="text-lg font-semibold text-white">
                  {{ cameraOverlayTitle }}
                </h2>
                <p class="mt-2 max-w-sm text-sm text-white/80">
                  {{ cameraOverlayDescription }}
                </p>
                <UButton
                  v-if="phase === 'idle' || phase === 'camera-error'"
                  class="mt-5 min-h-11"
                  size="lg"
                  :label="phase === 'camera-error' ? 'Coba buka kamera' : 'Aktifkan kamera'"
                  icon="i-lucide-camera"
                  @click="startScanner"
                />
              </div>

              <div class="absolute inset-x-0 top-0 flex justify-end p-3 sm:p-4">
                <UButton
                  v-if="phase === 'scanning'"
                  label="Hentikan kamera"
                  icon="i-lucide-camera-off"
                  color="neutral"
                  variant="solid"
                  size="sm"
                  class="min-h-11"
                  @click="stopScanner"
                />
              </div>
            </div>
            <div class="flex items-start gap-2.5 p-4 text-sm text-muted sm:px-5">
              <UIcon name="i-lucide-info" class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <p>Satu QR bisa mewakili beberapa orang. Pindai ulang untuk tiap orang dan tunggu hasil sebelum melanjutkan.</p>
            </div>
          </UCard>

          <!-- Panel hasil mengikuti tinggi kamera agar ruang kerja tidak terasa kosong. -->
          <UCard class="h-full">
            <div
              ref="resultPanel"
              role="status"
              aria-live="polite"
              aria-atomic="true"
              tabindex="-1"
              class="flex h-full min-h-[24rem] flex-col gap-5 focus-visible:outline-2 focus-visible:outline-primary"
            >
              <h2 class="text-sm font-semibold text-highlighted">
                Hasil pemeriksaan
              </h2>

              <template v-if="phase === 'result'">
                <div class="flex items-start gap-3">
                  <div
                    class="flex size-12 shrink-0 items-center justify-center rounded-full"
                    :class="resultKind === 'success' ? 'bg-success/10 text-success' : uncertainResult ? 'bg-warning/10 text-warning' : 'bg-error/10 text-error'"
                  >
                    <UIcon
                      :name="resultKind === 'success' ? 'i-lucide-check' : uncertainResult ? 'i-lucide-triangle-alert' : 'i-lucide-x'"
                      class="size-6"
                      aria-hidden="true"
                    />
                  </div>
                  <div>
                    <h3 class="text-lg font-semibold text-highlighted">
                      {{ resultKind === 'success' ? 'Pengunjung boleh masuk' : uncertainResult ? 'Hasil belum terkonfirmasi' : 'Check-in gagal' }}
                    </h3>
                    <p class="mt-1 text-sm text-muted">
                      {{ resultMessage }}
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

                <div
                  v-if="uncertainResult"
                  role="alert"
                  class="space-y-3 rounded-lg border border-warning/30 bg-warning/5 p-4"
                >
                  <p class="text-sm font-medium text-highlighted">
                    Jangan pindai tiket ini lagi dulu.
                  </p>
                  <p class="text-sm text-muted">
                    {{ user?.role === 'admin' ? 'Periksa riwayat tiket ini sebelum melanjutkan.' : 'Minta admin memastikan status tiket ini di Riwayat Check-in sebelum melanjutkan.' }}
                  </p>
                  <UButton
                    v-if="user?.role === 'admin' && scannedTicketId"
                    :to="{ path: '/checkins', query: { ticket: scannedTicketId } }"
                    target="_blank"
                    label="Periksa riwayat tiket"
                    icon="i-lucide-history"
                    color="warning"
                    variant="soft"
                    class="min-h-11"
                  />
                  <UCheckbox
                    v-model="statusChecked"
                    label="Status tiket sudah saya konfirmasi dengan admin"
                  />
                </div>

                <UButton
                  v-if="!uncertainResult || statusChecked"
                  label="Scan berikutnya"
                  icon="i-lucide-scan-line"
                  size="lg"
                  block
                  class="min-h-11"
                  @click="scanNext"
                />
                <p v-if="!uncertainResult" class="text-sm text-muted">
                  Lanjutkan setelah pengunjung melewati gate agar tidak terhitung dua kali.
                </p>
              </template>

              <div v-else class="flex flex-1 flex-col items-center justify-center py-6 text-center">
                <UIcon
                  :name="phase === 'checking' || phase === 'starting' || phase === 'preparing' ? 'i-lucide-loader-circle' : 'i-lucide-ticket'"
                  class="mb-4 size-8 text-dimmed"
                  :class="{ 'animate-spin motion-reduce:animate-none': phase === 'checking' || phase === 'starting' || phase === 'preparing' }"
                  aria-hidden="true"
                />
                <h3 class="font-medium text-highlighted">
                  {{ phase === 'preparing' ? 'Menyiapkan pemindai' : phase === 'starting' ? 'Menghubungkan kamera' : phase === 'checking' ? 'Mencocokkan tiket' : 'Siap memindai' }}
                </h3>
                <p class="mt-2 max-w-xs text-sm text-muted">
                  {{ phase === 'preparing' ? 'Kontrol kamera akan aktif setelah pemindai siap.' : phase === 'starting' ? 'Izinkan akses kamera jika diminta browser.' : phase === 'checking' ? 'Server sedang memvalidasi tiket dan mencatat satu pengunjung.' : 'Hasil validasi dan sisa pengunjung akan tampil di sini setelah QR terbaca.' }}
                </p>
              </div>
            </div>
          </UCard>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
