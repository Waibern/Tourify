<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import type { DashboardRecentOrder } from '~/types/tourify'

const props = defineProps<{
  orders: DashboardRecentOrder[]
  loading: boolean
}>()

const searchQuery = ref('')
// Select memakai string kosong untuk menghapus pilihan, jadi semua wahana pakai nilai khusus.
const allWahanaValue = '__all_wahana__'
const wahanaFilter = ref(allWahanaValue)
const visitDateFilter = ref('')

// Filter memakai field pesanan terbaru yang sudah tersedia di dashboard.
const wahanaOptions = computed(() => [
  { label: 'Semua wahana', value: allWahanaValue },
  ...Array.from(new Set(props.orders.map(order => order.wahana_name)))
    .sort((a, b) => a.localeCompare(b, 'id-ID'))
    .map(name => ({ label: name, value: `wahana:${name}` }))
])
const hasActiveFilters = computed(() => !!searchQuery.value.trim() || wahanaFilter.value !== allWahanaValue || !!visitDateFilter.value)
const filteredOrders = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase('id-ID')

  return props.orders.filter((order) => {
    const searchableFields = [order.id, `#${order.id}`, order.username, order.wahana_name]
    const matchesSearch = !query || searchableFields.some(field => field.toLocaleLowerCase('id-ID').includes(query))
    const matchesWahana = wahanaFilter.value === allWahanaValue || `wahana:${order.wahana_name}` === wahanaFilter.value
    const matchesVisitDate = !visitDateFilter.value || order.visit_date.slice(0, 10) === visitDateFilter.value

    return matchesSearch && matchesWahana && matchesVisitDate
  })
})

function resetFilters() {
  searchQuery.value = ''
  wahanaFilter.value = allWahanaValue
  visitDateFilter.value = ''
}

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
}, {
  id: 'actions',
  header: 'Detail',
  enableHiding: false
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
    <!-- Kontrol terpisah dari ASCII reveal agar mengetik tidak mengulang animasi tabel. -->
    <div
      v-if="orders.length"
      role="search"
      aria-label="Pencarian dan filter pesanan terbaru"
      class="mb-4 flex flex-wrap items-end gap-3"
    >
      <UFormField label="Cari pesanan" class="w-full sm:min-w-56 sm:flex-1">
        <UInput
          v-model="searchQuery"
          class="w-full"
          icon="i-lucide-search"
          placeholder="ID, pelanggan, atau wahana…"
          aria-label="Cari berdasarkan ID, pelanggan, atau wahana"
        />
      </UFormField>
      <UFormField label="Wahana" class="w-full sm:w-auto">
        <USelect
          v-model="wahanaFilter"
          :items="wahanaOptions"
          class="w-full sm:min-w-44"
          aria-label="Filter berdasarkan wahana"
        />
      </UFormField>
      <UFormField label="Tanggal kunjungan" class="w-full sm:w-auto">
        <UInput
          v-model="visitDateFilter"
          type="date"
          class="w-full sm:min-w-44"
          aria-label="Filter berdasarkan tanggal kunjungan"
        />
      </UFormField>
      <UButton
        v-if="hasActiveFilters"
        label="Reset"
        color="neutral"
        variant="ghost"
        @click="resetFilters"
      />
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
      <p v-else-if="!filteredOrders.length" role="status" class="text-sm text-muted">
        Tidak ada pesanan yang cocok. Ubah kata kunci atau filter.
      </p>
      <div v-else>
        <div class="hidden overflow-x-auto xl:block">
          <UTable
            :data="filteredOrders"
            :columns="columns"
            class="min-w-[880px] shrink-0"
            :ui="{
              base: 'table-fixed border-separate border-spacing-0',
              thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
              tbody: '[&>tr]:last:[&>td]:border-b-0',
              th: 'border-y border-default first:border-l last:border-r',
              td: 'border-b border-default'
            }"
          >
            <!-- Aksi baris membuka tiket yang sama pada halaman pesanan. -->
            <template #actions-cell="{ row }">
              <UButton
                label="Lihat detail"
                icon="i-lucide-eye"
                color="neutral"
                variant="ghost"
                size="sm"
                :to="{ path: '/orders', query: { ticket: row.original.id } }"
                :aria-label="`Lihat detail tiket #${row.original.id} milik ${row.original.username}`"
              />
            </template>
          </UTable>
        </div>
        <div class="divide-y divide-default xl:hidden">
          <article v-for="order in filteredOrders" :key="order.id" class="space-y-2 py-3 first:pt-0 last:pb-0">
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
            <!-- Aksi tetap tersedia saat tabel berubah menjadi daftar di layar kecil. -->
            <div class="flex justify-end">
              <UButton
                label="Lihat detail"
                icon="i-lucide-eye"
                color="neutral"
                variant="ghost"
                size="sm"
                class="min-h-11"
                :to="{ path: '/orders', query: { ticket: order.id } }"
                :aria-label="`Lihat detail tiket #${order.id} milik ${order.username}`"
              />
            </div>
          </article>
        </div>
      </div>
    </AsciiReveal>
  </UCard>
</template>
