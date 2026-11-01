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

    <div v-if="loading" class="text-sm text-muted" role="status">
      Memuat pemesanan…
    </div>
    <!-- Keadaan kosong menjelaskan bahwa rentang tanggal bisa diganti. -->
    <div v-else-if="!orders.length" class="text-sm text-muted">
      Belum ada pemesanan pada rentang tanggal ini. Coba pilih tanggal lain di atas.
    </div>
    <div v-else class="overflow-x-auto">
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
  </UCard>
</template>
