<script setup lang="ts">
import type { DashboardAnalyticsPoint } from '~/types/tourify'

const props = defineProps<{
  series: DashboardAnalyticsPoint[]
  loading: boolean
}>()

const numberFormat = new Intl.NumberFormat('id-ID')
const shortDateFormat = new Intl.DateTimeFormat('id-ID', {
  day: 'numeric',
  month: 'short'
})
const longDateFormat = new Intl.DateTimeFormat('id-ID', {
  day: 'numeric',
  month: 'short',
  year: '2-digit'
})

// Cari hari tersibuk dari jumlah tiket terjual.
const rankedDays = computed(() => {
  const useLongDate = props.series.length > 90

  return props.series
    .filter(point => point.ticket_count > 0)
    .map(point => {
      const date = new Date(point.date + 'T12:00:00')

      return {
        key: point.date,
        date: (useLongDate ? longDateFormat : shortDateFormat).format(date),
        tickets: point.ticket_count
      }
    })
    .sort((left, right) => right.tickets - left.tickets || left.key.localeCompare(right.key))
})

const busiestDay = computed(() => rankedDays.value[0] ?? null)
// Grafik menampilkan maksimal tujuh hari terpadat supaya tetap mudah dibaca.
const chartData = computed(() => rankedDays.value
  .slice(0, 7)
  .sort((left, right) => left.key.localeCompare(right.key))
  .map(({ date, tickets }) => ({ date, tickets })))
</script>

<template>
  <UCard
    data-dashboard-card
    :ui="{
      root: 'h-full border border-default bg-default',
      body: 'p-0! overflow-hidden rounded-b-[inherit]'
    }"
  >
    <div class="px-5 pt-4">
      <h2 class="text-sm font-medium text-highlighted">
        Hari tersibuk
      </h2>
    </div>
    <AsciiReveal :loading="loading" origin="pointer">
      <template #skeleton>
        <div class="space-y-3 px-5 pb-4 pt-3">
          <div class="h-9 w-28" aria-hidden="true" />
          <div class="h-[220px] w-full" aria-hidden="true" />
        </div>
      </template>

      <div v-if="busiestDay" class="px-5 pb-3 pt-2">
        <p class="text-3xl font-semibold tabular-nums text-highlighted">
          {{ busiestDay.date }}
        </p>
        <p class="mt-0.5 text-sm text-muted">
          {{ numberFormat.format(busiestDay.tickets) }} tiket terjual
        </p>
      </div>
      <p v-else class="px-5 pb-3 pt-2 text-sm text-muted">
        Belum ada tiket terjual pada rentang ini.
      </p>

      <div v-if="chartData.length" class="px-3 sm:px-4" data-ascii-chart>
        <!-- Batang membandingkan volume tiket pada tujuh tanggal terpadat. -->
        <UChart
          type="bar"
          :data="chartData"
          index="date"
          :categories="[{ key: 'tickets', label: 'Tiket terjual', color: 'primary' }]"
          :legend="false"
          :x-axis="true"
          :y-axis="true"
          :grid="true"
          :format="(value: number) => numberFormat.format(value)"
          :height="220"
          :initial-width="420"
          class="-my-px"
          :aria-label="`Tiket terjual per tanggal; hari tersibuk ${busiestDay?.date}`"
        />
      </div>
    </AsciiReveal>
  </UCard>
</template>
