<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import { getPaginationRowModel } from '@tanstack/table-core'
import type { TableColumn } from '@nuxt/ui'
import type { DashboardCustomer, DashboardCustomersApiResponse } from '~/types/tourify'

const api = useTourifyApi()
const route = useRoute()
// ID dari notifikasi harus cocok persis, supaya pelanggan #6 tidak tertukar dengan #16.
const customerId = computed(() => typeof route.query.customer === 'string' && /^[1-9]\d{0,9}$/.test(route.query.customer) ? route.query.customer : '')
const UAvatar = resolveComponent('UAvatar')
const customers = ref<DashboardCustomer[]>([])
const search = ref('')
const sort = ref('newest')
const loading = ref(true)
const errorMessage = ref('')
const table = useTemplateRef('table')
const columnVisibility = ref()
const pagination = ref({ pageIndex: 0, pageSize: 10 })
let requestId = 0

// Pembaruan otomatis tetap menampilkan daftar lama sampai data baru siap.
async function loadCustomers(background = false) {
  const request = ++requestId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const response = await api.get<DashboardCustomersApiResponse>('dashboard_customers.php')
    if (request !== requestId) return
    if (JSON.stringify(customers.value) !== JSON.stringify(response.customers ?? [])) {
      customers.value = response.customers ?? []
    }
    errorMessage.value = ''
  } catch (error) {
    if (request === requestId) errorMessage.value = getTourifyErrorMessage(error, 'Daftar pelanggan gagal dimuat.')
  } finally {
    if (request === requestId) loading.value = false
  }
}

// Avatar menampilkan foto akun; inisial jadi cadangan kalau foto belum tersedia.
function avatarText(username: string): string {
  return username.trim().slice(0, 2).toLocaleUpperCase('id-ID') || '?'
}

function formatRupiah(amount: number): string {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0
  }).format(amount)
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('id-ID', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  }).format(new Date(value))
}

const filteredCustomers = computed(() => {
  const query = search.value.trim().toLocaleLowerCase('id-ID')
  const rows = customers.value.filter(customer =>
    (!customerId.value || String(customer.id) === customerId.value)
    && (!query
      || customer.username.toLocaleLowerCase('id-ID').includes(query)
      || String(customer.id).includes(query))
  )

  return rows.sort((a, b) => {
    if (sort.value === 'orders') return b.order_count - a.order_count
    if (sort.value === 'value') return b.booking_value - a.booking_value
    return b.created_at.localeCompare(a.created_at)
  })
})

// Tautan pelanggan baru tidak tersaring oleh pencarian atau halaman tabel sebelumnya.
watch(customerId, () => {
  search.value = ''
})
watch([search, sort, customerId], () => {
  pagination.value.pageIndex = 0
})

const columns: TableColumn<DashboardCustomer>[] = [{
  accessorKey: 'id',
  header: 'ID'
}, {
  accessorKey: 'username',
  header: 'Pelanggan',
  cell: ({ row }) => h('div', { class: 'flex items-center gap-3' }, [
    h(UAvatar, {
      src: api.assetUrl(row.original.profile_image),
      text: avatarText(row.original.username),
      alt: `Foto profil ${row.original.username}`,
      color: 'primary',
      size: 'md'
    }),
    h('p', { class: 'truncate font-medium text-highlighted' }, row.original.username)
  ])
}, {
  accessorKey: 'created_at',
  header: 'Tanggal daftar',
  cell: ({ row }) => formatDate(row.original.created_at)
}, {
  accessorKey: 'order_count',
  header: 'Pesanan'
}, {
  accessorKey: 'ticket_count',
  header: 'Tiket dipesan'
}, {
  accessorKey: 'booking_value',
  header: 'Nilai pemesanan',
  cell: ({ row }) => formatRupiah(row.original.booking_value)
}]

// Menu ini memakai kolom tabel yang betul-betul tersedia di data Tourify.
const displayItems = computed(() => table.value?.tableApi?.getAllColumns()
  .filter(column => column.getCanHide())
  .map(column => ({
    label: String(column.columnDef.header ?? column.id),
    type: 'checkbox' as const,
    checked: column.getIsVisible(),
    onUpdateChecked(checked: boolean) {
      table.value?.tableApi?.getColumn(column.id)?.toggleVisibility(checked)
    },
    onSelect(event?: Event) {
      event?.preventDefault()
    }
  })))

onMounted(() => loadCustomers())
useDashboardAutoRefresh(() => loadCustomers(true), () => loading.value)
</script>

<template>
  <UDashboardPanel id="customers">
    <template #header>
      <UDashboardNavbar title="Pelanggan">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UButton
            label="Muat ulang"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            :loading="loading"
            @click="loadCustomers()"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div class="flex min-h-0 flex-1 flex-col gap-4">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <UInput
            v-model="search"
            class="w-full sm:max-w-sm"
            icon="i-lucide-search"
            placeholder="Cari username atau ID…"
            aria-label="Cari pelanggan berdasarkan username atau ID"
          />

          <div class="flex flex-wrap items-center gap-2">
            <USelect
              v-model="sort"
              :items="[
                { label: 'Terbaru', value: 'newest' },
                { label: 'Pesanan terbanyak', value: 'orders' },
                { label: 'Nilai tertinggi', value: 'value' }
              ]"
              aria-label="Urutkan pelanggan"
              class="min-w-44"
            />
            <UDropdownMenu :items="displayItems" :content="{ align: 'end' }">
              <UButton
                label="Tampilan"
                color="neutral"
                variant="outline"
                trailing-icon="i-lucide-settings-2"
              />
            </UDropdownMenu>
          </div>
        </div>

        <!-- Pembatas dari notifikasi terlihat dan bisa dilepas tanpa kehilangan halaman pelanggan. -->
        <div v-if="customerId" class="flex flex-wrap items-center gap-3 text-sm text-muted">
          <span>Pelanggan #{{ customerId }}</span>
          <UButton
            label="Lihat semua pelanggan"
            color="neutral"
            variant="link"
            :to="{ query: { ...route.query, customer: undefined } }"
            class="min-h-11"
          />
        </div>

        <UAlert
          v-if="errorMessage"
          color="error"
          variant="subtle"
          :description="errorMessage"
          :actions="[{ label: 'Coba lagi', onClick: () => loadCustomers() }]"
        />

        <!-- Polling tidak mengembalikan petugas ke halaman pertama tabel. -->
        <UTable
          ref="table"
          v-model:column-visibility="columnVisibility"
          v-model:pagination="pagination"
          :pagination-options="{ getPaginationRowModel: getPaginationRowModel(), autoResetPageIndex: false }"
          :data="filteredCustomers"
          :columns="columns"
          :loading="loading"
          class="shrink-0"
          :ui="{
            base: 'table-fixed border-separate border-spacing-0',
            thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
            tbody: '[&>tr]:last:[&>td]:border-b-0',
            th: 'py-3 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r',
            td: 'py-3 border-b border-default align-middle',
            separator: 'h-0'
          }"
        >
          <template #empty>
            {{ search || customerId ? 'Pelanggan tidak ditemukan.' : 'Belum ada akun pelanggan.' }}
          </template>
        </UTable>

        <div class="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-default pt-4">
          <p class="text-sm text-muted">
            {{ filteredCustomers.length }} pelanggan
          </p>
          <UPagination
            :page="pagination.pageIndex + 1"
            :items-per-page="pagination.pageSize"
            :total="filteredCustomers.length"
            @update:page="(page: number) => pagination.pageIndex = page - 1"
          />
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
