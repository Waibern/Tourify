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
const currencyFormat = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  maximumFractionDigits: 0
})

// Chart mengambil tiga wahana teratas dan tetap mendukung respons API lama.
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

// Legenda dan donut memakai urutan, jumlah, dan warna yang sama.
const ticketShareLegend = computed<TicketShareItem[]>(() => rankedWahana.value.length > 0
  ? [
      ...rankedWahana.value.map((item, index) => ({
        label: item.name,
        value: item.ticket_count,
        rank: index + 1,
        color: rankedWahanaColors[index] ?? '#34d399'
      })),
      {
        label: 'Wahana lainnya',
        value: otherTicketCount.value,
        rank: null,
        color: otherWahanaColor
      }
    ]
  : [])
const ticketShare = computed(() => ticketShareLegend.value.map(({ label, value }) => ({ label, value })))
const ticketShareColors = computed(() => ticketShareLegend.value.map(item => item.color))
const ticketSharePercent = computed(() => totalTicketCount.value > 0
  ? Math.round(rankedTicketCount.value / totalTicketCount.value * 100)
  : 0)
const ticketShareCenterLabel = computed(() => rankedWahana.value.length === 3
  ? '3 teratas'
  : numberFormat.format(rankedWahana.value.length) + ' wahana')
const ticketShareGroupLabel = computed(() => 'Porsi tiket ' + numberFormat.format(rankedWahana.value.length) + ' wahana terlaris dan wahana lainnya')

// Rincian ini dipakai tooltip chart dan legenda yang bisa difokuskan dengan keyboard.
function ticketShareDetails(label: string) {
  const item = ticketShareLegend.value.find(entry => entry.label === label)
  if (!item) return label

  const share = totalTicketCount.value > 0
    ? Math.round(item.value / totalTicketCount.value * 100)
    : 0
  const ranking = item.rank ? 'Peringkat ' + numberFormat.format(item.rank) : 'Wahana lainnya'

  return ranking + ' · ' + numberFormat.format(item.value) + ' tiket · ' + share + '% dari total ' + numberFormat.format(totalTicketCount.value) + ' tiket'
}

function ticketShareTooltip(label: string) {
  const item = ticketShareLegend.value.find(entry => entry.label === label)
  return item ? item.label + '. ' + ticketShareDetails(label) + '.' : label
}

// Deskripsi menyebut jumlah tiket keseluruhan dan rincian tiap irisan.
const ticketShareDescription = computed(() => numberFormat.format(totalTicketCount.value) + ' tiket keseluruhan. ' + ticketShareLegend.value.map(item => ticketShareTooltip(item.label)).join(' '))
</script>

<template>
  <UCard
    data-dashboard-card
    :aria-busy="loading"
    :ui="{ root: 'overflow-hidden border border-default bg-default', body: 'p-0!' }"
  >
    <AsciiReveal :loading="loading" origin="pointer">
      <template #skeleton>
        <div class="relative flex min-h-[24rem] flex-col justify-between gap-6 p-5 sm:min-h-[22rem] sm:p-6 md:min-h-56 md:gap-8 md:p-8" role="status">
          <div class="space-y-3">
            <!-- Skeleton mengikuti hierarki judul yang baru. -->
            <USkeleton class="h-9 w-52 sm:h-10" aria-hidden="true" />
            <USkeleton class="h-6 w-3/5" aria-hidden="true" />
          </div>
          <!-- Sisakan tempat chart agar isi kartu tidak bergeser setelah data siap. -->
          <div class="relative z-10 order-2 flex w-full items-center gap-2.5 rounded-xl bg-black/45 p-3 shadow-xl shadow-black/40 backdrop-blur-md sm:gap-3 xl:absolute xl:right-7 xl:top-1/2 xl:order-none xl:w-auto xl:-translate-y-1/2" aria-hidden="true">
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
          <!-- Kerangka memakai susunan rapat yang sama saat data dimuat. -->
          <dl class="grid grid-cols-2 gap-4 sm:w-fit sm:grid-cols-[max-content_max-content_max-content] sm:gap-x-12">
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

      <div v-if="wahana" class="relative isolate flex min-h-[24rem] min-w-0 flex-col justify-between gap-6 overflow-hidden p-5 sm:min-h-[22rem] sm:p-6 md:min-h-56 md:gap-8 md:p-8">
        <img
          :src="wahanaImage(wahana.id)"
          :alt="`Foto ${wahana.name}`"
          class="absolute inset-0 z-0 size-full object-cover"
        >
        <div class="absolute inset-0 z-0 bg-gradient-to-r from-black/85 via-black/60 to-black/25" />
        <div class="absolute inset-0 z-0 bg-gradient-to-b from-black/30 via-transparent to-black/75" />

        <div class="relative z-10 order-1 min-w-0">
          <!-- Nama wahana menjadi teks pendamping judul utama. -->
          <h2 class="text-3xl font-semibold leading-tight tracking-tight text-white sm:text-4xl">
            Wahana terlaris
          </h2>
          <p class="mt-2 break-words text-lg font-medium text-white/85 sm:text-xl">
            {{ wahana.name }}
          </p>
        </div>

        <!-- Panel tetap berada di alur pada layar kecil dan mengambang di desktop. -->
        <div
          v-if="totalTicketCount > 0 && ticketShare.length > 0"
          class="relative z-10 order-2 flex w-full items-center gap-2.5 rounded-xl bg-black/55 p-3 shadow-xl shadow-black/40 backdrop-blur-md sm:gap-3 xl:absolute xl:right-7 xl:top-1/2 xl:order-none xl:w-auto xl:-translate-y-1/2"
          role="group"
          :aria-label="ticketShareGroupLabel"
        >
          <!-- Penanda ini memasukkan donut ke snapshot ASCII reveal. -->
          <div class="relative size-32 shrink-0" data-ascii-chart>
            <!-- Tooltip chart menampilkan nama, peringkat, jumlah, dan porsi tiap irisan. -->
            <UChart
              type="donut"
              :data="ticketShare"
              index="label"
              :categories="[{ key: 'value', label: 'Jumlah tiket' }]"
              :colors="ticketShareColors"
              :legend="false"
              :tooltip="{ portal: true, content: { side: 'top', sideOffset: 8 } }"
              :thickness="16"
              :height="128"
              :initial-width="128"
              :format="(value: number) => numberFormat.format(value)"
              aria-label="Diagram porsi tiket wahana"
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
                {{ ticketSharePercent }}%
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
                  class="flex min-w-0 items-start gap-1.5 rounded-sm py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/90 focus-visible:ring-offset-2 focus-visible:ring-offset-black"
                  role="group"
                  tabindex="0"
                  :aria-label="ticketShareTooltip(item.label)"
                >
                  <span
                    v-if="item.rank"
                    class="grid size-5 shrink-0 place-items-center rounded-sm text-[10px] font-bold tabular-nums text-emerald-950"
                    :style="{ backgroundColor: item.color }"
                    aria-hidden="true"
                  >
                    #{{ item.rank }}
                  </span>
                  <span
                    v-else
                    class="mt-1.5 size-2 shrink-0 rounded-full"
                    :style="{ backgroundColor: item.color }"
                    aria-hidden="true"
                  />
                  <span class="min-w-0 flex-1 break-words text-sm leading-5 text-white/90">{{ item.label }}</span>
                  <span class="shrink-0 text-sm font-semibold tabular-nums text-white">{{ numberFormat.format(item.value) }}</span>
                </div>
              </UTooltip>
            </div>
          </div>
        </div>

        <!-- Angka dikelompokkan rapat agar mudah dibandingkan. -->
        <dl class="relative z-10 order-3 grid grid-cols-2 gap-x-4 gap-y-5 sm:w-fit sm:grid-cols-[max-content_max-content_max-content] sm:gap-x-12">
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
          <!-- Judul tetap menonjol saat belum ada wahana terlaris. -->
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
