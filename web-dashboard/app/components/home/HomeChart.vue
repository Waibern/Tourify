<script setup lang="ts">
import { format, startOfWeek } from 'date-fns'
import type { DashboardAnalyticsPoint } from '~/types/tourify'
import type { Period } from '~/types'
import { analyticsChange } from '~/utils/analytics'

const props = defineProps<{
  series: DashboardAnalyticsPoint[]
  previousValue: number | null
  period: Period
  loading: boolean
}>()

const currencyFormat = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  maximumFractionDigits: 0
}).format

const hasBookings = computed(() => props.series.some(point => point.order_count > 0))

// Gabungkan nilai harian sesuai pilihan rentang tanggal.
const chartData = computed(() => {
  const groups = new Map<string, { date: string, amount: number }>()

  for (const point of props.series) {
    const date = new Date(point.date + 'T12:00:00')
    const bucket = props.period === 'weekly'
      ? startOfWeek(date, { weekStartsOn: 1 })
      : props.period === 'monthly'
        ? new Date(date.getFullYear(), date.getMonth(), 1)
        : date
    const key = props.period === 'daily'
      ? format(bucket, 'yyyy-MM-dd')
      : props.period === 'weekly'
        ? format(bucket, 'yyyy-MM-dd')
        : format(bucket, 'yyyy-MM')
    const label = props.period === 'daily'
      ? new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short' }).format(bucket)
      : props.period === 'weekly'
        ? 'Pekan ' + new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short' }).format(bucket)
        : new Intl.DateTimeFormat('id-ID', { month: 'short', year: 'numeric' }).format(bucket)
    const current = groups.get(key) ?? { date: label, amount: 0 }

    current.amount += point.booking_value
    groups.set(key, current)
  }

  return Array.from(groups.values())
})

const total = computed(() => props.series.reduce((sum, point) => sum + point.booking_value, 0))
// Tampilkan selisih dalam rupiah dan arah perubahannya dalam persen.
const totalDifference = computed(() => props.previousValue === null
  ? null
  : Math.abs(total.value - props.previousValue))
// Bandingkan omzet dengan rentang tanggal sebelumnya yang sama panjang.
const totalChange = computed(() => props.previousValue === null
  ? null
  : analyticsChange(total.value, props.previousValue))
</script>

<template>
  <!-- Klik kartu untuk membuka daftar pesanan yang diringkas di grafik. -->
  <NuxtLink
    to="/orders"
    class="group block h-full min-w-0 cursor-pointer rounded-xl focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white/50"
    aria-label="Buka Pesanan dan Tiket untuk melihat nilai pemesanan"
  >
    <!-- Kartu grafik memakai tampilan yang sama dengan kartu angka. -->
    <UCard data-dashboard-card :ui="{ root: 'h-full overflow-visible border border-default bg-default transition-colors group-hover:border-white/35', body: 'p-0! overflow-hidden rounded-b-[inherit]' }">
      <div class="px-6 pt-4">
        <p class="text-xs text-muted uppercase mb-1.5">
          Nilai pemesanan
        </p>
      </div>
      <AsciiReveal :loading="loading" origin="pointer">
        <template #skeleton>
          <!-- Sisakan ruang grafik supaya ukuran kartu tidak berubah saat memuat. -->
          <div class="px-6 pb-4">
            <div class="h-9" aria-hidden="true" />
          </div>
          <div class="px-6">
            <div class="aspect-[3/1] w-full" aria-hidden="true" />
          </div>
        </template>
        <div class="px-6 pb-4">
          <div class="flex flex-wrap items-center gap-3">
            <p class="text-3xl text-highlighted font-semibold">
              {{ currencyFormat(total) }}
            </p>
            <p
              v-if="totalChange && totalDifference !== null"
              class="text-sm tabular-nums"
              title="Dibandingkan rentang tanggal sebelumnya"
            >
              <span
                class="font-medium"
                :class="{
                  'text-primary': totalChange.color === 'success',
                  'text-error': totalChange.color === 'error',
                  'text-muted': totalChange.color === 'neutral'
                }"
              >
                {{ totalChange.label }} ({{ currencyFormat(totalDifference) }})
              </span>
              <span class="ms-1 text-muted">· vs periode sebelumnya</span>
            </p>
          </div>
        </div>
        <!-- Kalau belum ada pesanan, pengguna bisa mencoba tanggal lain. -->
        <div v-if="!hasBookings" class="flex flex-wrap items-center justify-between gap-3 px-6 pb-5 text-sm text-muted">
          <span>Belum ada pemesanan pada rentang tanggal ini. Coba pilih tanggal lain.</span>
          <span class="inline-flex items-center gap-1 text-primary">
            Lihat pesanan
            <UIcon name="i-lucide-arrow-up-right" class="size-4" aria-hidden="true" />
          </span>
        </div>
        <div v-else class="px-6" data-ascii-chart>
          <!-- Tanggal dan angka pada sumbu membantu membaca grafik. -->
          <UChart
            type="area"
            curve="monotone"
            :data="chartData"
            index="date"
            :categories="[{ key: 'amount', label: 'Nilai pemesanan', color: 'primary' }]"
            :x-axis="true"
            :y-axis="true"
            :grid="true"
            :format="currencyFormat"
            :height="220"
            :initial-width="1152"
            class="-my-px"
            aria-label="Grafik nilai pemesanan"
          />
        </div>
      </AsciiReveal>
    </UCard>
  </NuxtLink>
</template>
