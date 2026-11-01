<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import type { DashboardRecentOrder } from '~/types/tourify'

defineProps<{
  orders: DashboardRecentOrder[]
  loading: boolean
}>()

const currencyFormat = new Intl.NumberFormat('id-ID', {
  style: 'currency',
  currency: 'IDR',
  maximumFractionDigits: 0
})

function formatDateTime(value: string): string {
  const parts = value.replace('T', ' ').split(' ')
  const date = parts[0]?.split('-').reverse().join('-') ?? value
  const time = parts[1]?.slice(0, 5)

  return time ? date + ' ' + time : date
}

// Tabel merangkum pesanan yang tercatat pada rentang dashboard.
const columns: TableColumn<DashboardRecentOrder>[] = [{
  accessorKey: 'id',
  header: 'ID',
  cell: ({ row }) => '#' + row.original.id
}, {
  accessorKey: 'username',
  header: 'Pelanggan'
}, {
  accessorKey: 'wahana_name',
  header: 'Wahana'
}, {
  accessorKey: 'booking_date',
  header: 'Waktu pesan',
  cell: ({ row }) => formatDateTime(row.original.booking_date)
}, {
  accessorKey: 'visit_date',
  header: 'Tanggal kunjungan',
  cell: ({ row }) => formatDateTime(row.original.visit_date)
}, {
  accessorKey: 'quantity',
  header: 'Tiket'
}, {
  accessorKey: 'total_price',
  header: 'Nilai pesanan',
  cell: ({ row }) => currencyFormat.format(row.original.total_price)
}]
</script>

<template>
  <!-- Tabel pesanan memakai permukaan yang sama dengan kartu Ringkasan lain. -->
  <UCard data-dashboard-card>
    <template #header>
      <h2 class="font-semibold text-highlighted">
        Pemesanan terbaru
      </h2>
    </template>

    <!-- Status loading tetap dibaca screen reader saat skeleton tampil. -->
    <div v-if="loading" class="sr-only" role="status">
      Memuat pemesanan…
    </div>
    <!-- Reveal mencakup isi kartu saat data selesai dimuat. -->
    <AsciiReveal :loading="loading" origin="pointer">
      <template #skeleton>
        <div class="space-y-3" aria-hidden="true">
          <div class="h-10 rounded-md bg-elevated/50" />
          <div v-for="row in 4" :key="row" class="h-10 rounded-md bg-elevated/25" />
        </div>
      </template>
      <!-- Keadaan kosong menjelaskan bahwa rentang tanggal bisa diganti. -->
      <div v-if="!orders.length" class="text-sm text-muted">
        Belum ada pemesanan pada rentang tanggal ini. Coba pilih tanggal lain di atas.
      </div>
      <div v-else>
        <div class="hidden overflow-x-auto xl:block">
          <UTable
            :data="orders"
            :columns="columns"
            class="min-w-[760px] shrink-0"
            :ui="{
              base: 'table-fixed border-separate border-spacing-0',
              thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
              tbody: '[&>tr]:last:[&>td]:border-b-0',
              th: 'border-y border-default first:border-l last:border-r',
              td: 'border-b border-default'
            }"
          />
        </div>
        <div class="divide-y divide-default xl:hidden">
          <article v-for="order in orders" :key="order.id" class="space-y-2 py-3 first:pt-0 last:pb-0">
            <div class="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
              <p class="font-medium tabular-nums text-highlighted">
                Pesanan #{{ order.id }} · {{ order.username }}
              </p>
              <p class="text-sm text-muted">
                {{ formatDateTime(order.booking_date) }}
              </p>
            </div>
            <p class="break-words text-highlighted">
              {{ order.wahana_name }}
            </p>
            <div class="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm">
              <p class="text-muted">
                {{ order.quantity }} tiket · kunjungan {{ formatDateTime(order.visit_date) }}
              </p>
              <p class="font-medium tabular-nums text-highlighted">
                {{ currencyFormat.format(order.total_price) }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </AsciiReveal>
  </UCard>
</template>
