<script setup lang="ts">
import type { DashboardAnalytics, DashboardAnalyticsPoint, DashboardAnalyticsStats } from '~/types/tourify'
import { analyticsChange } from '~/utils/analytics'

const props = defineProps<{
  stats: DashboardAnalyticsStats | null
  previous: DashboardAnalyticsStats | null
  series: DashboardAnalyticsPoint[]
  comparison: DashboardAnalytics['comparison'] | null
  loading: boolean
}>()

const numberFormat = new Intl.NumberFormat('id-ID')
const dateFormat = new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short' })
function dateLabel(value: string): string {
  return dateFormat.format(new Date(value + 'T12:00:00'))
}

// Kunjungan jadi fokus utama; metrik lain tetap menuju daftar terkait.
const metrics = [
  { key: 'checkins', title: 'Pengunjung masuk', to: '/checkins' },
  { key: 'orders', title: 'Pesanan', to: '/orders' },
  { key: 'tickets', title: 'Tiket dipesan', to: '/orders' },
  { key: 'new_customers', title: 'Pelanggan baru', to: '/customers' }
] as const

const cards = computed(() => metrics.map((metric) => {
  const value = props.stats?.[metric.key] ?? null
  const previous = props.previous?.[metric.key] ?? null

  return {
    ...metric,
    value,
    previous,
    // Persentase hanya dipakai pada kartu Pengunjung masuk.
    change: metric.key === 'checkins' && value !== null && previous !== null
      ? analyticsChange(value, previous)
      : null
  }
}))
const featured = computed(() => cards.value[0]!)
const secondary = computed(() => cards.value.slice(1))
const comparisonLabel = computed(() => props.comparison
  ? `${dateLabel(props.comparison.start)}–${dateLabel(props.comparison.end)}`
  : '')

// Grafik kecil hanya muncul saat ada aktivitas, jadi keadaan kosong tetap ringkas.
const checkinData = computed(() => props.series.map(point => ({
  date: point.date,
  value: point.checkin_count
})))
const hasCheckins = computed(() => checkinData.value.some(point => point.value > 0))
</script>

<template>
  <!-- Kartu tetap berdampingan saat jendela separuh layar; baru ditumpuk di lebar HP. -->
  <section class="grid gap-3 min-[900px]:grid-cols-2" aria-label="Ringkasan aktivitas" :aria-busy="loading">
    <p v-if="loading" class="sr-only" role="status">
      Memuat ringkasan Tourify…
    </p>

    <!-- Kartu kunjungan tetap menonjol lewat isi, tanpa garis tepi hijau. -->
    <NuxtLink
      :to="featured.to"
      data-dashboard-card
      class="group block min-w-0 overflow-hidden rounded-xl border border-default bg-default transition-colors hover:border-white/35 hover:bg-elevated/40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-400"
      :aria-label="`${featured.title}: ${featured.value === null ? 'data belum tersedia' : numberFormat.format(featured.value)}. Buka riwayat check-in`"
    >
      <AsciiReveal :loading="loading" origin="pointer">
        <template #skeleton>
          <div class="h-44" aria-hidden="true" />
        </template>
        <div class="flex min-h-44 flex-col justify-between gap-3 p-4 sm:p-5">
          <div class="flex items-start justify-between gap-3">
            <div>
              <h2 class="text-base font-semibold text-highlighted">
                {{ featured.title }}
              </h2>
              <p class="mt-1 text-sm text-muted">
                Pada rentang tanggal terpilih
              </p>
            </div>
            <UIcon name="i-lucide-arrow-up-right" class="size-5 shrink-0 text-primary transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" aria-hidden="true" />
          </div>

          <div class="flex flex-wrap items-end justify-between gap-x-5 gap-y-2">
            <div class="flex items-center gap-3">
              <p class="text-4xl font-semibold tracking-tight text-highlighted tabular-nums">
                {{ featured.value === null ? '—' : numberFormat.format(featured.value) }}
              </p>
              <UBadge
                v-if="featured.change"
                :color="featured.change.color"
                variant="subtle"
                size="sm"
                class="rounded-full tabular-nums"
                :title="`Dibandingkan ${comparisonLabel}`"
              >
                {{ featured.change.label }}
              </UBadge>
            </div>
            <!-- Pembanding nol tidak perlu menambah teks di kartu. -->
            <p v-if="featured.previous !== 0" class="text-sm text-muted">
              {{ featured.previous === null ? 'Data pembanding belum tersedia' : `vs ${numberFormat.format(featured.previous)} sebelumnya` }}
            </p>
          </div>

          <div v-if="hasCheckins" class="h-[54px]" data-ascii-chart>
            <UChart
              type="area"
              curve="monotone"
              :data="checkinData"
              index="date"
              :categories="[{ key: 'value', label: featured.title, color: 'primary' }]"
              :x-axis="false"
              :y-axis="false"
              :grid="false"
              :height="54"
              :initial-width="520"
              :format="(value: number) => numberFormat.format(value)"
              :aria-label="`Tren harian ${featured.title.toLowerCase()}`"
            />
          </div>
          <p v-else class="text-sm text-muted">
            {{ featured.value === null ? 'Data kunjungan belum tersedia.' : 'Belum ada pengunjung pada rentang ini.' }}
          </p>
        </div>
      </AsciiReveal>
    </NuxtLink>

    <!-- Teks dan angka boleh turun baris saat area kartu menyempit di samping sidebar. -->
    <div class="grid gap-2">
      <NuxtLink
        v-for="card in secondary"
        :key="card.key"
        :to="card.to"
        data-dashboard-card
        class="group flex min-w-0 items-center rounded-xl border border-default bg-default px-4 py-3 transition-colors hover:border-white/35 hover:bg-elevated/40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-400"
        :aria-label="`${card.title}: ${card.value === null ? 'data belum tersedia' : numberFormat.format(card.value)}. Buka detail`"
      >
        <AsciiReveal :loading="loading" origin="pointer" class="min-w-0 flex-1">
          <template #skeleton>
            <div class="h-10" aria-hidden="true" />
          </template>
          <div class="flex min-w-0 flex-wrap items-center justify-between gap-x-3 gap-y-2">
            <div class="min-w-0">
              <h2 class="text-sm font-medium text-highlighted">
                {{ card.title }}
              </h2>
              <!-- Aturan pembanding nol sama untuk semua metrik. -->
              <p v-if="card.previous !== 0" class="text-sm text-muted">
                {{ card.previous === null ? 'Belum ada pembanding' : `vs ${numberFormat.format(card.previous)} sebelumnya` }}
              </p>
            </div>
            <div class="flex shrink-0 items-center gap-2">
              <p class="text-xl font-semibold text-highlighted tabular-nums">
                {{ card.value === null ? '—' : numberFormat.format(card.value) }}
              </p>
              <UIcon name="i-lucide-arrow-up-right" class="size-4 text-muted transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" aria-hidden="true" />
            </div>
          </div>
        </AsciiReveal>
      </NuxtLink>
    </div>
  </section>
</template>
