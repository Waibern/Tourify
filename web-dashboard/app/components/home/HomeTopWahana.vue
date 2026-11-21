<script setup lang="ts">
import type { DashboardTopWahana } from '~/types/tourify'
import { wahanaImage } from '~/utils/wahana-images'

const props = defineProps<{
  wahana: DashboardTopWahana | null
  topWahanaDistribution: DashboardTopWahana[]
  totalTickets: number | null
  loading: boolean
}>()

const numberFormat = new Intl.NumberFormat('id-ID')
const percentFormat = new Intl.NumberFormat('id-ID', {
  style: 'percent',
  maximumFractionDigits: 1
})
const ratingFormat = new Intl.NumberFormat('id-ID', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1
})
const currencyFormat = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  maximumFractionDigits: 0
})

// Tampilkan tiga wahana teratas dan tetap dukung format API lama.
const rankedWahana = computed(() => props.topWahanaDistribution.length > 0
  ? props.topWahanaDistribution.slice(0, 3)
  : props.wahana ? [props.wahana] : [])
const rankedTicketCount = computed(() => rankedWahana.value.reduce((total, item) => total + item.ticket_count, 0))
const totalTicketCount = computed(() => Math.max(props.totalTickets ?? 0, rankedTicketCount.value))
const otherTicketCount = computed(() => Math.max(totalTicketCount.value - rankedTicketCount.value, 0))
const rankedWahanaColors = ['#34d399', '#a3e635', '#10b981']
const otherWahanaColor = 'rgba(255, 255, 255, 0.58)'
type TicketShareItem = {
  label: string
  value: number
  rank: number | null
  color: string
}

// Tampilkan sisa tiket di baris Wahana lainnya jika ada.
const ticketShareLegend = computed<TicketShareItem[]>(() => rankedWahana.value.length > 0
  ? [
      ...rankedWahana.value.map((item, index) => ({
        label: item.name,
        value: item.ticket_count,
        rank: index + 1,
        color: rankedWahanaColors[index] ?? '#34d399'
      })),
      ...(otherTicketCount.value > 0
        ? [{
            label: 'Wahana lainnya',
            value: otherTicketCount.value,
            rank: null,
            color: otherWahanaColor
          }]
        : [])
    ]
  : [])
const ticketShare = computed(() => ticketShareLegend.value.map(({ label, value }) => ({ label, value })))
const ticketShareColors = computed(() => ticketShareLegend.value.map(item => item.color))
// Jumlah semua tiket ada di tengah grafik, sedangkan rinciannya ada di legenda.
const ticketShareCenterLabel = 'total tiket'
const ticketShareGroupLabel = computed(() => 'Distribusi ' + numberFormat.format(totalTicketCount.value) + ' tiket terjual per wahana')

// Hitung persentase dari total tiket pada tanggal yang dipilih.
function formatTicketShare(value: number): string {
  return percentFormat.format(totalTicketCount.value > 0 ? value / totalTicketCount.value : 0)
}

// Keterangan di atas grafik menampilkan rincian; legenda bisa dipilih dengan keyboard.
function ticketShareDetails(label: string) {
  const item = ticketShareLegend.value.find(entry => entry.label === label)
  if (!item) return label

  const share = formatTicketShare(item.value)
  const ranking = item.rank ? 'Peringkat ' + numberFormat.format(item.rank) : 'Wahana lainnya'

  return ranking + ' · ' + numberFormat.format(item.value) + ' tiket · ' + share + ' dari total ' + numberFormat.format(totalTicketCount.value) + ' tiket'
}

function ticketShareTooltip(label: string) {
  const item = ticketShareLegend.value.find(entry => entry.label === label)
  return item ? item.label + '. ' + ticketShareDetails(label) + '.' : label
}

// Pembaca layar mendapat jumlah total dan persentase tiap wahana.
const ticketShareDescription = computed(() => 'Total tiket terjual: ' + numberFormat.format(totalTicketCount.value) + '. ' + ticketShareLegend.value.map(item => ticketShareTooltip(item.label)).join(' '))
</script>

<template>
  <UCard
    data-dashboard-card
    :aria-busy="loading"
    :ui="{ root: 'overflow-hidden border border-default bg-default', body: 'p-0!' }"
  >
    <AsciiReveal :loading="loading" origin="pointer">
      <template #skeleton>
        <div data-wahana-hero class="relative flex min-h-[24rem] flex-col justify-between gap-6 p-5 sm:min-h-[22rem] sm:p-6 md:min-h-56 md:gap-8 md:p-8" role="status">
          <div class="space-y-3">
            <!-- Isi sementara mengikuti posisi judul dan subjudul. -->
            <USkeleton class="h-9 w-52 sm:h-10" aria-hidden="true" />
            <div class="flex flex-wrap items-center gap-2">
              <USkeleton class="h-6 w-3/5" aria-hidden="true" />
              <USkeleton class="h-7 w-28 rounded-full" aria-hidden="true" />
            </div>
          </div>
          <!-- Sisakan tempat grafik supaya isi kartu tidak bergeser saat data siap. -->
          <div data-wahana-chart class="relative z-10 order-3 flex w-full items-center gap-2.5 rounded-xl bg-black/45 p-3 shadow-xl shadow-black/40 backdrop-blur-md sm:gap-3" aria-hidden="true">
            <USkeleton class="size-32 shrink-0 rounded-full" />
            <div class="grid min-w-0 flex-1 gap-2 sm:w-52 sm:flex-none">
              <div class="flex items-center gap-1.5">
                <USkeleton class="size-5 shrink-0" />
                <USkeleton class="h-4 min-w-0 flex-1" />
                <USkeleton class="h-4 w-8 shrink-0" />
              </div>
              <div class="flex items-center gap-1.5">
                <USkeleton class="size-5 shrink-0" />
                <USkeleton class="h-4 min-w-0 flex-1" />
                <USkeleton class="h-4 w-8 shrink-0" />
              </div>
              <div class="flex items-center gap-1.5">
                <USkeleton class="size-5 shrink-0" />
                <USkeleton class="h-4 min-w-0 flex-1" />
                <USkeleton class="h-4 w-8 shrink-0" />
              </div>
              <div class="flex items-center gap-1.5">
                <USkeleton class="size-5 shrink-0" />
                <USkeleton class="h-4 min-w-0 flex-1" />
                <USkeleton class="h-4 w-8 shrink-0" />
              </div>
            </div>
          </div>
          <!-- Isi sementara mengikuti susunan kartu sebenarnya. -->
          <dl class="order-2 grid grid-cols-2 gap-4 sm:w-fit sm:grid-cols-[max-content_max-content_max-content] sm:gap-x-12">
            <div class="space-y-2">
              <USkeleton class="h-4 w-24" aria-hidden="true" />
              <USkeleton class="h-7 w-12" aria-hidden="true" />
            </div>
            <div class="space-y-2">
              <USkeleton class="h-4 w-20" aria-hidden="true" />
              <USkeleton class="h-7 w-12" aria-hidden="true" />
            </div>
            <div class="col-span-2 space-y-2 sm:col-span-1">
              <USkeleton class="h-4 w-32" aria-hidden="true" />
              <USkeleton class="h-7 w-36" aria-hidden="true" />
            </div>
          </dl>
          <span class="sr-only">Memuat wahana terlaris</span>
        </div>
      </template>

      <div v-if="wahana" data-wahana-hero class="relative isolate flex min-h-[24rem] min-w-0 flex-col justify-between gap-6 overflow-hidden p-5 sm:min-h-[22rem] sm:p-6 md:min-h-56 md:gap-8 md:p-8">
        <img
          :src="wahanaImage(wahana.id)"
          :alt="`Foto ${wahana.name}`"
          class="absolute inset-0 z-0 size-full object-cover"
        >
        <div class="absolute inset-0 z-0 bg-gradient-to-r from-black/85 via-black/60 to-black/25" />
        <div class="absolute inset-0 z-0 bg-gradient-to-b from-black/30 via-transparent to-black/75" />

        <div class="relative z-10 order-1 min-w-0">
          <!-- Tampilkan nama wahana di bawah judul. -->
          <h2 class="text-3xl font-semibold leading-tight tracking-tight text-white sm:text-4xl">
            Wahana terlaris
          </h2>
          <div class="mt-2 flex flex-wrap items-center gap-2.5">
            <p class="break-words text-lg font-medium text-white/85 sm:text-xl">
              {{ wahana.name }}
            </p>
            <!-- Tampilkan rating dan jumlah ulasan tanpa latar tambahan. -->
            <div
              class="inline-flex min-h-8 items-center gap-1.5 text-white"
            >
              <UIcon
                name="i-lucide-star"
                :class="wahana.rating_count > 0 ? 'fill-amber-300 text-amber-300' : 'text-white/65'"
                class="size-4 shrink-0"
                aria-hidden="true"
              />
              <span class="text-sm font-semibold tabular-nums">
                {{ wahana.average_rating === null ? 'Belum ada rating' : ratingFormat.format(wahana.average_rating) }}
              </span>
              <span v-if="wahana.rating_count > 0" class="text-xs text-white/75">
                · {{ numberFormat.format(wahana.rating_count) }} ulasan
              </span>
            </div>
          </div>
        </div>

        <!-- Di layar lebar, grafik di kanan; di layar sempit, lebarnya penuh. -->
        <div
          v-if="totalTicketCount > 0 && ticketShare.length > 0"
          data-wahana-chart
          class="relative z-10 order-3 flex w-full items-center gap-2.5 rounded-xl bg-black/55 p-3 shadow-xl shadow-black/40 backdrop-blur-md sm:gap-3"
          role="group"
          :aria-label="ticketShareGroupLabel"
        >
          <!-- Grafik ini ikut tampil dalam animasi ASCII. -->
          <div class="relative size-32 shrink-0" data-ascii-chart>
            <!-- Keterangan di atas grafik menampilkan nama, peringkat, jumlah, dan persentase wahana. -->
            <UChart
              type="donut"
              :data="ticketShare"
              index="label"
              :categories="[{ key: 'value', label: 'Tiket terjual' }]"
              :colors="ticketShareColors"
              :legend="false"
              :tooltip="{ portal: true, content: { side: 'top', sideOffset: 8 } }"
              :thickness="16"
              :height="128"
              :initial-width="128"
              :format="(value: number) => numberFormat.format(value)"
              aria-label="Diagram distribusi tiket terjual per wahana"
              :aria-description="ticketShareDescription"
              class="size-32"
            >
              <template #tooltip="{ rows }">
                <div class="grid max-w-56 gap-1.5">
                  <div v-for="row in rows" :key="row.label" class="space-y-0.5">
                    <p class="break-words text-sm font-medium text-highlighted">
                      {{ row.label }}
                    </p>
                    <p class="text-sm text-muted">
                      {{ ticketShareDetails(row.label) }}
                    </p>
                  </div>
                </div>
              </template>
            </UChart>
            <div class="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
              <span class="text-2xl font-semibold tabular-nums text-white">
                {{ numberFormat.format(totalTicketCount) }}
              </span>
              <span class="text-sm leading-tight text-white/80">
                {{ ticketShareCenterLabel }}
              </span>
            </div>
          </div>
          <div class="grid min-w-0 flex-1 gap-1 sm:w-52 sm:flex-none sm:gap-1.5">
            <div
              v-for="item in ticketShareLegend"
              :key="item.label"
              class="min-w-0"
            >
              <UTooltip :text="ticketShareTooltip(item.label)" :content="{ side: 'top', sideOffset: 6 }">
                <div
                  class="flex min-w-0 cursor-help items-start gap-1.5 rounded-sm py-1 transition-colors hover:bg-white/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/90 focus-visible:ring-offset-2 focus-visible:ring-offset-black"
                  role="group"
                  tabindex="0"
                  :aria-label="ticketShareTooltip(item.label)"
                >
                  <!-- Warna grafik ikut ditangkap supaya animasinya tetap utuh. -->
                  <div
                    v-if="item.rank"
                    class="relative size-5 shrink-0"
                    data-ascii-chart
                    aria-hidden="true"
                  >
                    <svg
                      viewBox="0 0 20 20"
                      class="absolute inset-0 size-full"
                      data-ascii="skip"
                      aria-hidden="true"
                    >
                      <rect
                        width="20"
                        height="20"
                        rx="3"
                        :fill="item.color"
                      />
                    </svg>
                    <span class="relative z-10 grid size-full place-items-center rounded-sm text-[10px] font-bold tabular-nums text-emerald-950">
                      #{{ item.rank }}
                    </span>
                  </div>
                  <div
                    v-else
                    class="relative mt-1.5 size-2 shrink-0"
                    data-ascii-chart
                    aria-hidden="true"
                  >
                    <svg
                      viewBox="0 0 8 8"
                      class="absolute inset-0 size-full"
                      data-ascii="skip"
                      aria-hidden="true"
                    >
                      <circle
                        cx="4"
                        cy="4"
                        r="4"
                        :fill="item.color"
                      />
                    </svg>
                  </div>
                  <span class="min-w-0 flex-1 break-words text-sm leading-5 text-white/90">{{ item.label }}</span>
                  <span class="shrink-0 text-right text-sm font-semibold tabular-nums text-white">
                    {{ numberFormat.format(item.value) }}
                    <span class="ms-1 text-xs font-normal text-white/75">· {{ formatTicketShare(item.value) }}</span>
                  </span>
                </div>
              </UTooltip>
            </div>
          </div>
        </div>

        <!-- Angka ditampilkan berdekatan supaya mudah dibandingkan. -->
        <dl class="relative z-10 order-2 grid grid-cols-2 gap-x-4 gap-y-5 sm:w-fit sm:grid-cols-[max-content_max-content_max-content] sm:gap-x-12">
          <div>
            <dt class="text-sm text-white/80">
              Tiket terjual
            </dt>
            <dd class="mt-1 text-xl font-semibold tabular-nums text-white">
              {{ numberFormat.format(wahana.ticket_count) }}
            </dd>
          </div>
          <div>
            <dt class="text-sm text-white/80">
              Pesanan
            </dt>
            <dd class="mt-1 text-xl font-semibold tabular-nums text-white">
              {{ numberFormat.format(wahana.order_count) }}
            </dd>
          </div>
          <div class="col-span-2 sm:col-span-1">
            <dt class="text-sm text-white/80">
              Nilai pemesanan
            </dt>
            <dd class="mt-1 text-xl font-semibold tabular-nums text-white">
              {{ currencyFormat.format(wahana.booking_value) }}
            </dd>
          </div>
        </dl>
      </div>

      <div v-else class="flex min-h-36 items-center px-5 py-6 sm:px-8">
        <div>
          <!-- Judul tetap terlihat saat belum ada wahana terlaris. -->
          <h2 class="text-2xl font-semibold text-highlighted">
            Wahana terlaris
          </h2>
          <p class="mt-2 text-sm text-muted">
            Belum ada pesanan wahana pada rentang tanggal ini.
          </p>
        </div>
      </div>
    </AsciiReveal>
  </UCard>
</template>

<style scoped>
/* Letakkan grafik di samping hanya kalau kartu cukup lebar. */
[data-wahana-hero] {
  container-type: inline-size;
}

@container (min-width: 50rem) {
  [data-wahana-hero] [data-wahana-chart] {
    position: absolute;
    order: 0;
    top: 50%;
    right: 1.75rem;
    width: auto;
    transform: translateY(-50%);
  }
}
</style>
