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

// Tampilkan tren pelanggan kalau jumlahnya berubah.
function customerTrend(current: number, previous: number) {
  const difference = current - previous

  if (difference === 0) return null

  if (difference > 0) {
    return {
      color: 'text-primary',
      label: `+${numberFormat.format(difference)} pelanggan`,
      description: `Jumlah pelanggan baru naik ${numberFormat.format(difference)}.`
    }
  }

  return {
    color: 'text-error',
    label: `-${numberFormat.format(Math.abs(difference))} pelanggan`,
    description: `Jumlah pelanggan baru turun ${numberFormat.format(Math.abs(difference))}.`
  }
}

// Jumlah wahana diambil dari katalog, bukan dari rentang tanggal.
const metrics = [
  { key: 'checkins', title: 'Pengunjung masuk', icon: 'i-lucide-scan-line', to: '/checkins' },
  { key: 'tickets', title: 'Tiket', icon: 'i-lucide-ticket', to: '/orders' },
  { key: 'wahana', title: 'Wahana', icon: 'i-lucide-map-pin', to: '/wahana' },
  { key: 'new_customers', title: 'Pelanggan baru', icon: 'i-lucide-user-round-plus', to: '/customers' }
] as const

const cards = computed(() => metrics.map((metric) => {
  const value = props.stats?.[metric.key] ?? null
  const previous = metric.key === 'wahana' ? null : props.previous?.[metric.key] ?? null

  return {
    ...metric,
    value,
    previous,
    // Tampilkan selisih jumlah pelanggan, bukan persentasenya.
    customerTrend: metric.key === 'new_customers' && value !== null && previous !== null
      ? customerTrend(value, previous)
      : null,
    // Persentase hanya tersedia untuk check-in dan tiket.
    change: (metric.key === 'checkins' || metric.key === 'tickets') && value !== null && previous !== null
      ? analyticsChange(value, previous)
      : null,
    ticketDifference: metric.key === 'tickets' && value !== null && previous !== null
      ? value - previous
      : null
  }
}))
const featured = computed(() => cards.value[0]!)
const secondary = computed(() => cards.value.slice(1))
const comparisonLabel = computed(() => props.comparison
  ? `${dateLabel(props.comparison.start)}–${dateLabel(props.comparison.end)}`
  : '')

// Tampilkan grafik kecil kalau ada aktivitas.
const checkinData = computed(() => props.series.map(point => ({
  date: point.date,
  value: point.checkin_count
})))
const hasCheckins = computed(() => checkinData.value.some(point => point.value > 0))
</script>

<template>
  <!-- Empat angka sejajar di desktop, dua kolom di tablet, dan satu kolom di HP. -->
  <section class="grid grid-cols-1 items-stretch gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="Ringkasan aktivitas" :aria-busy="loading">
    <p v-if="loading" class="sr-only" role="status">
      Memuat ringkasan Tourify…
    </p>

  <!-- Kartu pengunjung dibedakan lewat isinya, bukan garis hijau. -->
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
        <div class="grid grid-rows-[minmax(1.5rem,auto)_2.5rem_minmax(2.5rem,auto)_2rem] gap-2 p-4 sm:p-5">
          <!-- Samakan posisi judul, angka, dan keterangan di semua kartu. -->
          <div class="flex items-start justify-between gap-3">
            <h2 class="flex items-center gap-2 text-base font-semibold text-highlighted">
              <UIcon
                :name="featured.icon"
                data-ascii="fade"
                class="size-4 shrink-0 text-primary"
                aria-hidden="true"
              />
              <span>{{ featured.title }}</span>
            </h2>
            <UIcon name="i-lucide-arrow-up-right" class="size-5 shrink-0 text-primary transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" aria-hidden="true" />
          </div>

          <p class="text-4xl font-semibold leading-none tracking-tight text-highlighted tabular-nums">
            {{ featured.value === null ? '—' : numberFormat.format(featured.value) }}
          </p>

          <!-- Tampilkan perubahan di bawah angka supaya posisi semua kartu sama. -->
          <div class="text-sm leading-5">
            <p
              v-if="featured.change"
              class="font-medium tabular-nums"
              :class="featured.change.color === 'error' ? 'text-error' : featured.change.color === 'neutral' ? 'text-muted' : 'text-primary'"
            >
              {{ featured.change.label }}
            </p>
            <p class="text-muted">
              {{ featured.previous === null ? 'Data pembanding belum tersedia' : `vs ${numberFormat.format(featured.previous)} sebelumnya` }}
            </p>
          </div>

          <div v-if="hasCheckins" class="h-8" data-ascii-chart>
            <UChart
              type="area"
              curve="monotone"
              :data="checkinData"
              index="date"
              :categories="[{ key: 'value', label: featured.title, color: 'primary' }]"
              :x-axis="false"
              :y-axis="false"
              :grid="false"
              :height="32"
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

    <!-- Kartu angka lain dibuat setinggi kartu pengunjung supaya mudah dibandingkan. -->
    <NuxtLink
      v-for="card in secondary"
      :key="card.key"
      :to="card.to"
      data-dashboard-card
      class="group block min-w-0 overflow-hidden rounded-xl border border-default bg-default transition-colors hover:border-white/35 hover:bg-elevated/40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-neutral-400"
      :aria-label="`${card.title}: ${card.value === null ? 'data belum tersedia' : numberFormat.format(card.value)}${card.customerTrend ? `. ${card.customerTrend.description}` : ''}. Buka detail`"
    >
      <AsciiReveal :loading="loading" origin="pointer">
        <template #skeleton>
          <div class="h-44" aria-hidden="true" />
        </template>
        <div class="grid grid-rows-[minmax(1.5rem,auto)_2.5rem_minmax(2.5rem,auto)_2rem] gap-2 p-4 sm:p-5">
          <div class="flex items-start justify-between gap-3">
            <h2 class="flex items-center gap-2 text-base font-semibold text-highlighted">
              <UIcon
                :name="card.icon"
                data-ascii="fade"
                class="size-4 shrink-0 text-primary"
                aria-hidden="true"
              />
              <span>{{ card.title }}</span>
            </h2>
            <UIcon name="i-lucide-arrow-up-right" class="size-5 shrink-0 text-muted transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5" aria-hidden="true" />
          </div>

          <p class="text-4xl font-semibold leading-none tracking-tight text-highlighted tabular-nums">
            {{ card.value === null ? '—' : numberFormat.format(card.value) }}
          </p>

          <!-- Angka pembanding mengikuti jenis kartu dan rentang tanggal. -->
          <div class="text-sm leading-5">
            <p v-if="card.customerTrend" :class="card.customerTrend.color">
              {{ card.customerTrend.label }}
            </p>
            <template v-else-if="card.key === 'tickets' && card.ticketDifference !== null">
              <p
                class="font-medium tabular-nums"
                :class="card.change?.color === 'error' ? 'text-error' : card.change?.color === 'neutral' ? 'text-muted' : 'text-primary'"
              >
                {{ card.change?.label ?? (card.ticketDifference > 0 ? 'Baru' : '0%') }} ({{ numberFormat.format(Math.abs(card.ticketDifference)) }} tiket)
              </p>
              <p class="text-muted">vs {{ comparisonLabel || 'periode sebelumnya' }}</p>
            </template>
            <p v-else class="text-muted">
              {{ card.key === 'wahana' ? 'Total wahana dalam katalog' : 'Pada rentang tanggal terpilih' }}
            </p>
          </div>
        </div>
      </AsciiReveal>
    </NuxtLink>
  </section>
</template>
