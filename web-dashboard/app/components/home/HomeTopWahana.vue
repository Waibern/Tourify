<script setup lang="ts">
import type { DashboardTopWahana } from '~/types/tourify'
import { wahanaImage } from '~/utils/wahana-images'

defineProps<{
  wahana: DashboardTopWahana | null
  loading: boolean
}>()

const numberFormat = new Intl.NumberFormat('id-ID')
const currencyFormat = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  maximumFractionDigits: 0
})
</script>

<template>
  <UCard
    data-dashboard-card
    :aria-busy="loading"
    :ui="{ root: 'overflow-hidden border border-default bg-default', body: 'p-0!' }"
  >
    <AsciiReveal :loading="loading" origin="pointer">
      <template #skeleton>
        <div class="flex min-h-64 flex-col justify-between gap-8 p-5 sm:p-6 md:min-h-56 md:p-8" role="status">
          <div class="space-y-3">
            <!-- Skeleton mengikuti hierarki judul yang baru. -->
            <USkeleton class="h-9 w-52 sm:h-10" aria-hidden="true" />
            <USkeleton class="h-6 w-3/5" aria-hidden="true" />
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

      <div v-if="wahana" class="relative isolate flex min-h-64 min-w-0 flex-col justify-between gap-8 overflow-hidden p-5 sm:p-6 md:min-h-56 md:p-8">
        <img
          :src="wahanaImage(wahana.id)"
          :alt="`Foto ${wahana.name}`"
          class="absolute inset-0 z-0 size-full object-cover"
        >
        <div class="absolute inset-0 z-0 bg-gradient-to-r from-black/85 via-black/60 to-black/25" />
        <div class="absolute inset-0 z-0 bg-gradient-to-b from-black/30 via-transparent to-black/75" />

        <div class="relative z-10 min-w-0">
          <!-- Nama wahana menjadi teks pendamping judul utama. -->
          <h2 class="text-3xl font-semibold leading-tight tracking-tight text-white sm:text-4xl">
            Wahana terlaris
          </h2>
          <p class="mt-2 break-words text-lg font-medium text-white/85 sm:text-xl">
            {{ wahana.name }}
          </p>
        </div>

        <!-- Angka dikelompokkan rapat agar mudah dibandingkan. -->
        <dl class="relative z-10 grid grid-cols-2 gap-x-4 gap-y-5 sm:w-fit sm:grid-cols-[max-content_max-content_max-content] sm:gap-x-12">
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
