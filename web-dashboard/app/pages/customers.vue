<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import { getPaginationRowModel } from '@tanstack/table-core'
import type { RowSelectionState } from '@tanstack/table-core'
import type { TableColumn } from '@nuxt/ui'
import type { DashboardCustomer, DashboardCustomersApiResponse } from '~/types/tourify'

const api = useTourifyApi()
const route = useRoute()
// ID dari notifikasi harus cocok persis, supaya pelanggan #6 tidak tertukar dengan #16.
const customerId = computed(() => typeof route.query.customer === 'string' && /^[1-9]\d{0,9}$/.test(route.query.customer) ? route.query.customer : '')
const UAvatar = resolveComponent('UAvatar')
const UCheckbox = resolveComponent('UCheckbox')
const customers = ref<DashboardCustomer[]>([])
const search = ref('')
const sort = ref('newest')
const loading = ref(true)
const errorMessage = ref('')
const table = useTemplateRef('table')
const columnVisibility = ref()
const pagination = ref({ pageIndex: 0, pageSize: 10 })
// Seleksi memakai ID akun supaya tetap akurat saat urutan atau filter berubah.
const rowSelection = ref<RowSelectionState>({})
const deleteModalOpen = ref(false)
const deletingCustomers = ref(false)
const deleteError = ref('')
const currentMonth = ref(jakartaMonthKey())
let requestId = 0

// Periode pelanggan baru mengikuti tanggal Jakarta yang dipakai dashboard.
function jakartaMonthKey(): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    month: '2-digit',
    timeZone: 'Asia/Jakarta',
    year: 'numeric'
  }).formatToParts(new Date())
  const year = parts.find(part => part.type === 'year')?.value ?? ''
  const month = parts.find(part => part.type === 'month')?.value ?? ''
  return `${year}-${month}`
}

// Pembaruan otomatis tetap menampilkan daftar lama sampai data baru siap.
async function loadCustomers(background = false) {
  const request = ++requestId
  currentMonth.value = jakartaMonthKey()
  if (!background) {
    loading.value = true
    errorMessage.value = ''
    deleteError.value = ''
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

const numberFormat = new Intl.NumberFormat('id-ID')
const summaryCards = computed(() => {
  const allCustomers = customers.value
  const newThisMonth = allCustomers.filter(customer => customer.created_at.slice(0, 7) === currentMonth.value).length
  const customersWithOrders = allCustomers.filter(customer => customer.order_count > 0).length

  return [
    {
      label: 'Total pelanggan',
      value: numberFormat.format(allCustomers.length),
      hint: 'Seluruh akun pelanggan',
      icon: 'i-lucide-users-round'
    },
    {
      label: 'Pelanggan baru',
      value: numberFormat.format(newThisMonth),
      hint: 'Terdaftar bulan ini',
      icon: 'i-lucide-user-round-plus'
    },
    {
      label: 'Sudah memesan',
      value: numberFormat.format(customersWithOrders),
      hint: 'Pernah membuat pesanan',
      icon: 'i-lucide-ticket-check'
    }
  ]
})

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

// Kartu HP mengikuti halaman dan urutan yang sama dengan tabel desktop.
const mobileCustomers = computed(() => {
  const start = pagination.value.pageIndex * pagination.value.pageSize
  return filteredCustomers.value.slice(start, start + pagination.value.pageSize)
})

function updateCustomerSelection(customer: DashboardCustomer, selected: boolean) {
  if (customer.order_count > 0) return

  const id = String(customer.id)
  const selection = { ...rowSelection.value }
  if (selected) selection[id] = true
  else rowSelection.value = Object.fromEntries(
    Object.entries(selection).filter(([selectedId]) => selectedId !== id)
  )
  if (selected) rowSelection.value = selection
}

// Hanya akun tanpa pesanan yang bisa dipilih; riwayat pelanggan tetap utuh.
const selectedCustomerIds = computed(() => Object.entries(rowSelection.value)
  .filter(([, selected]) => selected)
  .map(([id]) => Number(id))
  .filter(id => customers.value.some(customer => customer.id === id && customer.order_count === 0)))

// Hapus terpilih memakai endpoint admin dan muat ulang daftar setelah berhasil.
async function deleteSelectedCustomers() {
  if (!selectedCustomerIds.value.length || deletingCustomers.value) return
  deletingCustomers.value = true
  deleteError.value = ''
  try {
    await api.delete<{ success: boolean }>('dashboard_customers.php', { ids: selectedCustomerIds.value })
    const deletedIds = new Set(selectedCustomerIds.value)
    requestId += 1
    customers.value = customers.value.filter(customer => !deletedIds.has(customer.id))
    rowSelection.value = {}
    deleteModalOpen.value = false
  } catch (error) {
    deleteError.value = getTourifyErrorMessage(error, 'Akun pelanggan gagal dihapus. Muat ulang daftar lalu coba lagi.')
  } finally {
    deletingCustomers.value = false
  }
}

// Tautan pelanggan baru tidak tersaring oleh pencarian atau halaman tabel sebelumnya.
watch(customerId, () => {
  search.value = ''
})
watch([search, sort, customerId], () => {
  pagination.value.pageIndex = 0
})

const columns: TableColumn<DashboardCustomer>[] = [{
  id: 'select',
  header: ({ table: tableInstance }) => h(UCheckbox, {
    'aria-label': 'Pilih semua pelanggan di halaman ini',
    'modelValue': tableInstance.getIsAllPageRowsSelected(),
    'indeterminate': tableInstance.getIsSomePageRowsSelected(),
    'onUpdate:modelValue': (value: boolean) => tableInstance.toggleAllPageRowsSelected(!!value)
  }),
  cell: ({ row }) => h(UCheckbox, {
    'aria-label': `Pilih pelanggan ${row.original.username}`,
    'modelValue': row.getIsSelected(),
    'disabled': row.original.order_count > 0,
    'onUpdate:modelValue': (value: boolean) => row.toggleSelected(!!value)
  })
}, {
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
        <DashboardSummaryCards :cards="summaryCards" :loading="loading" />

        <div class="flex flex-wrap items-center justify-between gap-2">
          <UInput
            v-model="search"
            class="w-full sm:max-w-sm"
            icon="i-lucide-search"
            placeholder="Cari username atau ID…"
            aria-label="Cari pelanggan berdasarkan username atau ID"
          />

          <div class="flex flex-wrap items-center gap-2">
            <UButton
              v-if="selectedCustomerIds.length"
              :label="`Hapus terpilih (${selectedCustomerIds.length})`"
              icon="i-lucide-trash-2"
              color="error"
              variant="subtle"
              @click="deleteModalOpen = true"
            />
            <CustomersDeleteModal
              v-model:open="deleteModalOpen"
              :count="selectedCustomerIds.length"
              :loading="deletingCustomers"
              :error="deleteError"
              @confirm="deleteSelectedCustomers"
            />
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
          v-model:row-selection="rowSelection"
          :pagination-options="{ getPaginationRowModel: getPaginationRowModel(), autoResetPageIndex: false }"
          :row-selection-options="{ enableRowSelection: row => row.original.order_count === 0 }"
          :get-row-id="row => String(row.id)"
          :data="filteredCustomers"
          :columns="columns"
          :loading="loading"
          class="hidden shrink-0 xl:block"
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

        <!-- Seleksi hapus tetap tersedia di HP dan akun berpesanan tetap terlindungi. -->
        <div class="space-y-3 xl:hidden">
          <p v-if="loading && !customers.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat pelanggan…
          </p>
          <p v-else-if="!mobileCustomers.length" class="py-6 text-center text-sm text-muted">
            {{ search || customerId ? 'Pelanggan tidak ditemukan.' : 'Belum ada akun pelanggan.' }}
          </p>
          <article
            v-for="customer in mobileCustomers"
            :key="customer.id"
            class="rounded-xl border border-default bg-default p-4"
          >
            <div class="flex items-center justify-between gap-3">
              <div class="flex min-w-0 items-center gap-3">
                <UAvatar
                  :src="api.assetUrl(customer.profile_image)"
                  :text="avatarText(customer.username)"
                  :alt="`Foto profil ${customer.username}`"
                  color="primary"
                  size="md"
                />
                <div class="min-w-0">
                  <p class="truncate font-medium text-highlighted">
                    {{ customer.username }}
                  </p>
                  <p class="mt-1 text-sm text-muted">
                    ID {{ customer.id }} · {{ formatDate(customer.created_at) }}
                  </p>
                </div>
              </div>
              <UCheckbox
                :model-value="Boolean(rowSelection[String(customer.id)])"
                :disabled="customer.order_count > 0"
                :aria-label="`Pilih pelanggan ${customer.username} untuk dihapus`"
                class="min-h-11 min-w-11 justify-center"
                @update:model-value="(value: boolean) => updateCustomerSelection(customer, value)"
              />
            </div>
            <dl class="mt-4 grid grid-cols-2 gap-3 border-t border-default pt-3 text-sm">
              <div>
                <dt class="text-muted">
                  Pesanan
                </dt>
                <dd class="mt-1 tabular-nums text-highlighted">
                  {{ customer.order_count }}
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  Tiket dipesan
                </dt>
                <dd class="mt-1 tabular-nums text-highlighted">
                  {{ customer.ticket_count }}
                </dd>
              </div>
              <div class="col-span-2">
                <dt class="text-muted">
                  Nilai pemesanan
                </dt>
                <dd class="mt-1 font-medium tabular-nums text-highlighted">
                  {{ formatRupiah(customer.booking_value) }}
                </dd>
              </div>
            </dl>
          </article>
        </div>

        <p class="text-sm text-muted">
          Akun yang punya riwayat pesanan tidak bisa dipilih agar data transaksi tetap tersimpan.
        </p>

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
