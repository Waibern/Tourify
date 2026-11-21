<script setup lang="ts">
import { h, resolveComponent } from 'vue'
import type { RowSelectionState } from '@tanstack/table-core'
import type { TableColumn } from '@nuxt/ui'
import type { DashboardOrder, DashboardOrdersApiResponse, DashboardOrderTrendPoint } from '~/types/tourify'
import { wahanaImage } from '~/utils/wahana-images'
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'

const api = useTourifyApi()
const route = useRoute()
const router = useRouter()
const UCheckbox = resolveComponent('UCheckbox')
// Tautan riwayat memfilter pesanan ke ID tiket yang dipilih.
const ticketId = computed(() => {
  const ticketQuery = route.query.ticket
  if (typeof ticketQuery !== 'string' || !/^[1-9]\d{0,9}$/.test(ticketQuery)) {
    return ''
  }

  return ticketQuery
})
let openedTicketId = ''
const rows = ref<DashboardOrder[]>([])
const search = ref('')
const query = refDebounced(search, 300)
const date = ref('')
const page = ref(1)
const total = ref(0)
const perPage = ref(20)
const summaryStats = ref<DashboardOrdersApiResponse['stats'] | null>(null)
const orderTrend = ref<DashboardOrderTrendPoint[]>([])
const loading = ref(true)
const errorMessage = ref('')
// Tampilkan daftar dulu; kartu bergambar bisa dipilih lewat menu Tampilan.
const viewMode = ref<'cards' | 'list'>('list')
let requestId = 0
const status = ref('all')
const selected = ref<DashboardOrder | null>(null)
const detailOpen = ref(false)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()
const detailSurfaceSelector = '[data-slot="content"][role="dialog"]'
const table = useTemplateRef('table')
const columnVisibility = ref()
const rowSelection = ref<RowSelectionState>({})
const deleteModalOpen = ref(false)
const deletingTickets = ref(false)
const deleteError = ref('')
let detailReveal: AsciiRevealController | null = null
let closingDetail = false
const statusItems = [
  { label: 'Semua status', value: 'all' },
  { label: 'Aktif', value: 'Active' },
  { label: 'Terpakai', value: 'Used' },
  { label: 'Kedaluwarsa', value: 'Expire' }
]
// Tabel dan kartu memakai pesan kosong yang sama.
const emptyOrdersMessage = computed(() => {
  if (search.value || date.value || status.value !== 'all') {
    return 'Tidak ada pesanan sesuai filter.'
  }

  if (ticketId.value) {
    return `Tiket #${ticketId.value} tidak ditemukan.`
  }

  return 'Pesanan akan tampil setelah tiket dipesan.'
})

// Detail memakai pesanan dari tabel yang sedang ditampilkan.
function showDetail(order: DashboardOrder) {
  selected.value = order
  detailOpen.value = true
}

// Tiket terpakai tidak dapat dihapus agar riwayat kunjungan tetap tersimpan.
function canDeleteTicket(order: DashboardOrder): boolean {
  return order.can_delete
}

// Ubah pilihan berdasarkan ID supaya pilihan tetap tepat setelah daftar diperbarui.
function updateTicketSelection(order: DashboardOrder, isSelected: boolean) {
  if (!canDeleteTicket(order)) return

  if (isSelected) {
    rowSelection.value = {
      ...rowSelection.value,
      [order.id]: true
    }
    return
  }

  rowSelection.value = Object.fromEntries(
    Object.entries(rowSelection.value).filter(([id]) => id !== order.id)
  )
}

// Hanya kirim ID dari halaman aktif dan tiket yang masih bisa dihapus.
const selectedTicketIds = computed(() => {
  const deletableIds = new Set(rows.value.filter(canDeleteTicket).map(order => order.id))
  return Object.entries(rowSelection.value)
    .filter(([id, isSelected]) => isSelected && deletableIds.has(id))
    .map(([id]) => id)
})

// Saat dialog dibuka, animasinya selesai dulu sebelum bisa ditutup.
async function revealOrderDetail() {
  if (reducedMotion.value === 'reduce' || !asciiAnimations.value) return

  const surface = document.querySelector<HTMLElement>(detailSurfaceSelector)
  if (!surface) return

  detailReveal?.destroy()
  const controller = createAsciiReveal(surface.parentElement ?? surface, {
    preset: 'decode',
    surfaces: detailSurfaceSelector,
    duration: 'cinematic',
    speed: 1.2,
    origin: 'pointer',
    lazy: false
  })
  detailReveal = controller
  try {
    await controller.reveal({ within: surface })
  } catch {
    // Tetap tampilkan detail kalau animasi gagal.
    if (detailReveal === controller) {
      controller.destroy()
      detailReveal = null
    }
  }
}

async function closeOrderDetail() {
  if (closingDetail) return
  closingDetail = true

  const controller = detailReveal
  const surface = document.querySelector<HTMLElement>(detailSurfaceSelector)
  try {
    if (controller && asciiAnimations.value) await controller.hide(surface ? { within: surface } : {})
  } catch {
    // Tetap tutup dialog kalau animasi gagal.
  }
  if (detailReveal === controller) {
    controller?.destroy()
    detailReveal = null
  }

  detailOpen.value = false
  closingDetail = false
}

function handleDetailOpenChange(open: boolean) {
  if (open) {
    detailOpen.value = true
    return
  }

  void closeOrderDetail()
}

// Hentikan animasi saat meninggalkan halaman dengan dialog yang masih terbuka.
onBeforeUnmount(() => {
  detailReveal?.destroy()
  detailReveal = null
})

// Hentikan animasi detail kalau pengguna mematikannya di tab lain.
watch(asciiAnimations, (enabled) => {
  if (enabled) return
  detailReveal?.destroy()
  detailReveal = null
})

// Reset menghapus semua filter, termasuk ID dari tautan riwayat.
function resetFilters() {
  search.value = ''
  date.value = ''
  status.value = 'all'
  void router.replace({ query: { ...route.query, ticket: undefined } })
}

// Pembaruan otomatis tidak mengosongkan daftar; hasil lama diabaikan saat filter berubah.
async function loadRows(background = false) {
  const request = ++requestId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const params = new URLSearchParams({
      q: query.value.trim(),
      date: date.value,
      page: String(page.value),
      status: status.value === 'all' ? '' : status.value,
      ticket_id: ticketId.value
    })
    const response = await api.get<DashboardOrdersApiResponse>(`dashboard_orders.php?${params}`)
    if (request !== requestId) return
    rows.value = response.orders
    const deletableIds = new Set(response.orders.filter(canDeleteTicket).map(order => order.id))
    rowSelection.value = Object.fromEntries(
      Object.entries(rowSelection.value).filter(([id, isSelected]) => isSelected && deletableIds.has(id))
    )
    total.value = response.total
    perPage.value = response.per_page
    summaryStats.value = response.stats
    orderTrend.value = response.order_trend ?? []
    page.value = response.page
    errorMessage.value = ''
    // Perbarui status check-in di detail bersama daftar pesanan.
    if (detailOpen.value && selected.value) {
      selected.value = response.orders.find(order => order.id === selected.value?.id) ?? selected.value
    }
    // Buka detail dari tautan sekali saja; pembaruan data tidak membukanya lagi.
    const linkedOrder = response.orders.find(order => order.id === ticketId.value)
    if (linkedOrder && openedTicketId !== ticketId.value) {
      openedTicketId = ticketId.value
      showDetail(linkedOrder)
    }
  } catch (error) {
    if (request === requestId) errorMessage.value = getTourifyErrorMessage(error, 'Data gagal dimuat. Coba lagi.')
  } finally {
    if (request === requestId) loading.value = false
  }
}

// Hapus pilihan secara massal setelah konfirmasi admin.
async function deleteSelectedTickets() {
  const ids = [...selectedTicketIds.value]
  if (!ids.length || deletingTickets.value) return

  deletingTickets.value = true
  deleteError.value = ''
  try {
    const response = await api.delete<{ success: boolean, deleted_count: number, message?: string }>(
      'dashboard_orders.php',
      { ids }
    )
    if (!response.success || response.deleted_count !== ids.length) {
      throw new Error(response.message || 'Sebagian tiket gagal dihapus. Muat ulang daftar lalu coba lagi.')
    }

    requestId += 1
    rowSelection.value = {}
    deleteModalOpen.value = false
    if (selected.value && ids.includes(selected.value.id)) {
      await closeOrderDetail()
      selected.value = null
    }
    await loadRows()
  } catch (error) {
    deleteError.value = getTourifyErrorMessage(error, 'Tiket gagal dihapus. Muat ulang daftar lalu coba lagi.')
  } finally {
    deletingTickets.value = false
  }
}

// Mulai dari halaman pertama setelah filter berubah.
watch(ticketId, () => {
  openedTicketId = ''
})
watch([query, date, status, ticketId], () => {
  rowSelection.value = {}
  if (page.value !== 1) page.value = 1
  else loadRows()
})
watch(page, () => {
  rowSelection.value = {}
  loadRows()
})
onMounted(() => loadRows())
useDashboardAutoRefresh(() => loadRows(true), () => loading.value)

function formatDate(value: string, withTime = false): string {
  const [day, time] = value.split(' ')
  const formatted = (day ?? '').split('-').reverse().join('-')
  return withTime ? `${formatted} ${(time ?? '').slice(0, 8)}` : formatted
}
// Gunakan total yang tersimpan supaya perubahan harga wahana tidak mengubah riwayat.
function formatRupiah(value: number): string {
  return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(value)
}

const numberFormat = new Intl.NumberFormat('id-ID')
const summaryCards = computed(() => [
  {
    label: 'Total pesanan',
    value: summaryStats.value ? numberFormat.format(summaryStats.value.orders) : '—',
    hint: 'Semua pesanan',
    icon: 'i-lucide-receipt',
    // Total pesanan dihitung sepanjang waktu, sementara grafik menampilkan 14 hari terakhir.
    trend: orderTrend.value.map(point => ({ date: point.date, value: point.order_count })),
    trendLabel: 'Pesanan per hari · 14 hari terakhir'
  },
  {
    label: 'Tiket dipesan',
    value: summaryStats.value ? numberFormat.format(summaryStats.value.tickets) : '—',
    hint: 'Akumulasi seluruh tiket',
    icon: 'i-lucide-ticket'
  },
  {
    label: 'Nilai pemesanan',
    value: summaryStats.value ? formatRupiah(summaryStats.value.booking_value) : '—',
    hint: 'Akumulasi transaksi',
    icon: 'i-lucide-banknote'
  }
])

function statusLabel(value: DashboardOrder['status']): string {
  return { Active: 'Aktif', Used: 'Terpakai', Expire: 'Kedaluwarsa' }[value]
}

// Tampilkan kolom yang dipilih lewat menu Tampilan.
function isOrderColumnVisible(id: string): boolean {
  return columnVisibility.value?.[id] !== false
}

const columns: TableColumn<DashboardOrder>[] = [
  {
    id: 'select',
    enableHiding: false,
    header: ({ table: tableInstance }) => h(UCheckbox, {
      'aria-label': 'Pilih semua tiket yang bisa dihapus di halaman ini',
      'modelValue': tableInstance.getIsAllPageRowsSelected(),
      'indeterminate': tableInstance.getIsSomePageRowsSelected(),
      'onUpdate:modelValue': (value: boolean) => tableInstance.toggleAllPageRowsSelected(!!value)
    }),
    cell: ({ row }) => h(UCheckbox, {
      'aria-label': `Pilih tiket #${row.original.id} untuk dihapus`,
      'modelValue': row.getIsSelected(),
      'disabled': !canDeleteTicket(row.original),
      'onUpdate:modelValue': (value: boolean) => row.toggleSelected(!!value)
    })
  },
  { accessorKey: 'id', header: 'Tiket / pelanggan', enableHiding: false, cell: ({ row }) => `#${row.original.id}` },
  { accessorKey: 'wahana_name', header: 'Wahana' },
  { accessorKey: 'visit_date', header: 'Kunjungan', cell: ({ row }) => formatDate(row.original.visit_date) },
  { accessorKey: 'total_price', header: 'Total', cell: ({ row }) => formatRupiah(row.original.total_price) },
  { accessorKey: 'checked_in_quantity', header: 'Check-in' },
  { accessorKey: 'status', header: 'Status tiket' },
  { id: 'actions', header: 'Detail', enableHiding: false }
]

// Pilihan kartu, daftar, dan kolom ada di menu Tampilan yang sama.
const displayItems = computed(() => {
  const items = [
    { label: 'Tampilan pesanan', type: 'label' as const },
    {
      label: 'Kartu bergambar',
      icon: viewMode.value === 'cards' ? 'i-lucide-check' : 'i-lucide-panels-top-left',
      onSelect() {
        viewMode.value = 'cards'
      }
    },
    {
      label: 'Daftar ringkas',
      icon: viewMode.value === 'list' ? 'i-lucide-check' : 'i-lucide-list',
      onSelect() {
        viewMode.value = 'list'
      }
    }
  ]
  const columns = table.value?.tableApi?.getAllColumns().filter(column => column.getCanHide())

  if (viewMode.value === 'list' && columns?.length) {
    return [
      ...items,
      { type: 'separator' as const },
      { label: 'Kolom tabel', type: 'label' as const },
      ...columns.map(column => ({
        label: String(column.columnDef.header ?? column.id),
        type: 'checkbox' as const,
        checked: column.getIsVisible(),
        onUpdateChecked(checked: boolean) {
          table.value?.tableApi?.getColumn(column.id)?.toggleVisibility(checked)
        },
        onSelect(event?: Event) {
          event?.preventDefault()
        }
      }))
    ]
  }

  return items
})
</script>

<template>
  <UDashboardPanel id="orders">
    <template #header>
      <UDashboardNavbar title="Pesanan & Tiket">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UButton
            label="Perbarui"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            :loading="loading"
            @click="loadRows()"
          />
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="flex min-h-0 flex-1 flex-col gap-4">
        <DashboardSummaryCards :cards="summaryCards" :loading="loading" />

        <!-- Bedakan tanggal kunjungan dari tanggal pemesanan. -->
        <div class="flex flex-wrap items-end gap-3">
          <UFormField label="Cari pesanan" class="w-full sm:min-w-64 sm:flex-1">
            <UInput
              v-model="search"
              class="w-full"
              icon="i-lucide-search"
              placeholder="Nomor tiket, pelanggan, atau wahana…"
              aria-label="Cari pesanan dan tiket"
            />
          </UFormField>
          <UFormField label="Status tiket" class="min-w-36 flex-1 sm:flex-none">
            <USelect
              v-model="status"
              :items="statusItems"
              aria-label="Filter status tiket"
              class="w-full"
            />
          </UFormField>
          <UFormField label="Tanggal kunjungan" class="min-w-40 flex-1 sm:flex-none">
            <UInput
              v-model="date"
              type="date"
              aria-label="Filter tanggal kunjungan"
              class="w-full"
            />
          </UFormField>
          <div class="flex items-center gap-2">
            <UButton
              v-if="search || date || status !== 'all' || ticketId"
              label="Reset"
              color="neutral"
              variant="ghost"
              @click="resetFilters"
            />
            <OrdersDeleteModal
              v-model:open="deleteModalOpen"
              :count="selectedTicketIds.length"
              :loading="deletingTickets"
              :error="deleteError"
              @confirm="deleteSelectedTickets"
            >
              <UButton
                v-if="selectedTicketIds.length"
                :label="`Hapus tiket (${selectedTicketIds.length})`"
                icon="i-lucide-trash-2"
                color="error"
                variant="subtle"
                @click="deleteModalOpen = true"
              />
            </OrdersDeleteModal>
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

        <div v-if="ticketId" class="flex items-center gap-2 text-sm text-muted">
          <UBadge color="neutral" variant="subtle">
            Tiket #{{ ticketId }}
          </UBadge>
          <UButton
            label="Lihat semua pesanan"
            color="neutral"
            variant="link"
            size="sm"
            @click="resetFilters"
          />
        </div>

        <!-- Beri alasan kenapa tiket yang sudah dipakai tidak bisa dicentang. -->
        <p class="text-xs text-muted">
          Tiket yang sudah dipakai atau memiliki riwayat check-in tidak bisa dihapus.
        </p>

        <UAlert
          v-if="errorMessage"
          color="error"
          variant="subtle"
          :description="errorMessage"
          :actions="[{ label: 'Coba lagi', onClick: () => loadRows() }]"
        />
        <!-- Nomor tiket tetap terlihat saat tabel digeser dan bisa membuka detail. -->
        <UTable
          ref="table"
          v-model:column-visibility="columnVisibility"
          v-model:row-selection="rowSelection"
          :row-selection-options="{ enableRowSelection: row => canDeleteTicket(row.original) }"
          :get-row-id="row => String(row.id)"
          :data="rows"
          :columns="columns"
          :loading="loading"
          :class="viewMode === 'list' ? 'hidden shrink-0 xl:block' : 'hidden'"
          :ui="{
            base: 'w-full border-separate border-spacing-0',
            thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
            tbody: '[&>tr]:last:[&>td]:border-b-0',
            th: 'py-3 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r first:sticky first:left-0 first:z-20 first:bg-elevated max-sm:last:hidden',
            td: 'min-w-32 py-3 border-b border-default align-middle whitespace-normal first:sticky first:left-0 first:z-10 first:bg-default max-sm:last:hidden',
            separator: 'h-0'
          }"
        >
          <template #empty>
            {{ emptyOrdersMessage }}
          </template>
          <!-- Detail menampilkan data dari tiket yang dipilih. -->
          <template #id-cell="{ row }">
            <div class="min-w-0">
              <button
                type="button"
                class="rounded font-semibold text-highlighted underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                :aria-label="`Lihat detail tiket #${row.original.id} milik ${row.original.username}`"
                @click="showDetail(row.original)"
              >
                #{{ row.original.id }}
              </button>
              <p class="mt-1 break-words text-sm text-muted">
                {{ row.original.username }}
              </p>
            </div>
          </template>
          <template #wahana_name-cell="{ row }">
            <p class="font-medium text-highlighted">
              {{ row.original.wahana_name }}
            </p>
            <p class="mt-1 text-sm text-muted">
              {{ row.original.quantity }} pengunjung
            </p>
          </template>
          <template #total_price-cell="{ row }">
            <p class="font-medium tabular-nums text-highlighted">
              {{ formatRupiah(row.original.total_price) }}
            </p>
            <p class="mt-1 text-sm text-muted">
              {{ row.original.payment_method }}
            </p>
          </template>
          <template #checked_in_quantity-cell="{ row }">
            <p class="font-medium tabular-nums text-highlighted">
              {{ row.original.checked_in_quantity }} <span class="font-normal text-muted">/ {{ row.original.quantity }}</span>
            </p>
            <p class="mt-1 text-sm text-muted">
              {{ orderCheckinLabel(row.original.checked_in_quantity, row.original.quantity) }}
            </p>
          </template>
          <template #status-cell="{ row }">
            <UBadge :color="row.original.status === 'Active' ? 'success' : row.original.status === 'Used' ? 'primary' : 'neutral'" :icon="row.original.status === 'Active' ? 'i-lucide-ticket' : row.original.status === 'Used' ? 'i-lucide-circle-check' : 'i-lucide-clock'" variant="subtle">
              {{ statusLabel(row.original.status) }}
            </UBadge>
          </template>
          <template #actions-cell="{ row }">
            <UButton
              label="Lihat detail"
              icon="i-lucide-eye"
              color="neutral"
              variant="ghost"
              size="sm"
              :aria-label="`Lihat detail tiket #${row.original.id} milik ${row.original.username}`"
              @click="showDetail(row.original)"
            />
          </template>
        </UTable>

        <!-- Kartu menampilkan ringkasan tiket dan tombol utamanya. -->
        <div v-if="viewMode === 'cards'" class="space-y-3">
          <p v-if="loading && !rows.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat pesanan…
          </p>
          <p v-else-if="!rows.length" class="py-6 text-center text-sm text-muted">
            {{ emptyOrdersMessage }}
          </p>
          <article
            v-for="order in rows"
            :key="order.id"
            class="rounded-xl border border-default bg-default p-4"
          >
            <div class="relative -mx-4 -mt-4 mb-4 h-32 overflow-hidden rounded-t-xl">
              <img
                :src="wahanaImage(order.wahana_id)"
                alt=""
                aria-hidden="true"
                class="absolute inset-0 h-full w-full object-cover"
              >
              <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/15 to-transparent" />
              <p class="absolute inset-x-4 bottom-3 break-words font-semibold text-white drop-shadow-sm">
                {{ order.wahana_name }}
              </p>
            </div>
            <div class="flex items-start justify-between gap-3">
              <div class="flex min-w-0 items-start gap-2">
                <UCheckbox
                  :model-value="Boolean(rowSelection[String(order.id)])"
                  :disabled="!canDeleteTicket(order)"
                  :aria-label="`Pilih tiket #${order.id} milik ${order.username} untuk dihapus`"
                  class="min-h-11 min-w-11 justify-center"
                  @update:model-value="(value: boolean) => updateTicketSelection(order, value)"
                />
                <div class="min-w-0">
                  <button
                    type="button"
                    class="min-h-11 rounded font-semibold text-highlighted underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                    :aria-label="`Lihat detail tiket #${order.id} milik ${order.username}`"
                    @click="showDetail(order)"
                  >
                    Tiket #{{ order.id }}
                  </button>
                  <p class="break-words text-sm text-muted">
                    {{ order.username }}
                  </p>
                </div>
              </div>
              <UBadge :color="order.status === 'Active' ? 'success' : order.status === 'Used' ? 'primary' : 'neutral'" variant="subtle">
                {{ statusLabel(order.status) }}
              </UBadge>
            </div>

            <dl class="mt-3 grid grid-cols-2 gap-x-3 gap-y-3 border-t border-default pt-3 text-sm">
              <div>
                <dt class="text-muted">
                  Tanggal kunjungan
                </dt>
                <dd class="mt-1 text-highlighted">
                  {{ formatDate(order.visit_date) }}
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  Pengunjung
                </dt>
                <dd class="mt-1 text-highlighted">
                  {{ order.quantity }} orang
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  Check-in
                </dt>
                <dd class="mt-1 tabular-nums text-highlighted">
                  {{ order.checked_in_quantity }} / {{ order.quantity }}
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  Total
                </dt>
                <dd class="mt-1 break-words font-medium tabular-nums text-highlighted">
                  {{ formatRupiah(order.total_price) }}
                </dd>
              </div>
            </dl>
            <div class="mt-3 flex items-center justify-between gap-3 border-t border-default pt-3">
              <span class="text-sm text-muted">{{ order.payment_method }}</span>
              <UButton
                label="Lihat detail"
                icon="i-lucide-eye"
                color="neutral"
                variant="outline"
                class="min-h-11"
                :aria-label="`Lihat detail tiket #${order.id} milik ${order.username}`"
                @click="showDetail(order)"
              />
            </div>
          </article>
        </div>

        <!-- Tampilan HP memakai satu kolom tanpa banner foto. -->
        <section
          v-if="viewMode === 'list'"
          aria-label="Daftar pesanan"
          class="divide-y divide-default border-y border-default xl:hidden"
        >
          <p v-if="loading && !rows.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat pesanan…
          </p>
          <p v-else-if="!rows.length" class="py-6 text-center text-sm text-muted">
            {{ emptyOrdersMessage }}
          </p>
          <article
            v-for="order in rows"
            :key="order.id"
            class="flex flex-wrap items-center gap-x-5 gap-y-3 px-1 py-3 sm:px-3"
          >
            <div class="flex min-w-36 flex-1 items-center gap-2">
              <UCheckbox
                :model-value="Boolean(rowSelection[String(order.id)])"
                :disabled="!canDeleteTicket(order)"
                :aria-label="`Pilih tiket #${order.id} milik ${order.username} untuk dihapus`"
                class="min-h-11 min-w-11 justify-center"
                @update:model-value="(value: boolean) => updateTicketSelection(order, value)"
              />
              <div class="min-w-0">
                <button
                  type="button"
                  class="min-h-11 rounded font-semibold text-highlighted underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                  :aria-label="`Lihat detail tiket #${order.id} milik ${order.username}`"
                  @click="showDetail(order)"
                >
                  Tiket #{{ order.id }}
                </button>
                <p class="break-words text-sm text-muted">
                  {{ order.username }}
                  <template v-if="isOrderColumnVisible('wahana_name')">
                    · {{ order.wahana_name }}
                  </template>
                </p>
              </div>
            </div>

            <dl class="grid w-full grid-cols-2 gap-x-4 gap-y-2 text-sm sm:w-auto sm:flex sm:flex-wrap sm:gap-x-5">
              <div v-if="isOrderColumnVisible('visit_date')">
                <dt class="text-muted">
                  Kunjungan
                </dt>
                <dd class="mt-1 text-highlighted">
                  {{ formatDate(order.visit_date) }}
                </dd>
              </div>
              <div v-if="isOrderColumnVisible('wahana_name')">
                <dt class="text-muted">
                  Pengunjung
                </dt>
                <dd class="mt-1 text-highlighted">
                  {{ order.quantity }} orang
                </dd>
              </div>
              <div v-if="isOrderColumnVisible('checked_in_quantity')">
                <dt class="text-muted">
                  Check-in
                </dt>
                <dd class="mt-1 tabular-nums text-highlighted">
                  {{ order.checked_in_quantity }} / {{ order.quantity }}
                </dd>
              </div>
              <div v-if="isOrderColumnVisible('total_price')">
                <dt class="text-muted">
                  Total
                </dt>
                <dd class="mt-1 font-medium tabular-nums text-highlighted">
                  {{ formatRupiah(order.total_price) }}
                </dd>
              </div>
            </dl>

            <div class="ml-auto flex items-center gap-3">
              <UBadge
                v-if="isOrderColumnVisible('status')"
                :color="order.status === 'Active' ? 'success' : order.status === 'Used' ? 'primary' : 'neutral'"
                variant="subtle"
              >
                {{ statusLabel(order.status) }}
              </UBadge>
              <UButton
                label="Detail"
                icon="i-lucide-eye"
                color="neutral"
                variant="ghost"
                class="min-h-11"
                :aria-label="`Lihat detail tiket #${order.id} milik ${order.username}`"
                @click="showDetail(order)"
              />
            </div>
          </article>
        </section>

        <div class="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-default pt-4">
          <p class="text-sm text-muted">
            {{ total }} pesanan · {{ perPage }} per halaman
          </p>
          <UPagination
            v-if="total > perPage"
            :page="page"
            :items-per-page="perPage"
            :total="total"
            @update:page="(value: number) => page = value"
          />
        </div>
      </div>
      <!-- Dialog detail memakai animasi saat dibuka dan ditutup. -->
      <UModal
        :open="detailOpen"
        :title="selected ? `Pesanan #${selected.id}` : 'Detail pesanan'"
        description="Rincian tiket dan progres check-in pesanan."
        :ui="{
          content: 'bg-default bg-[linear-gradient(135deg,rgba(255,255,255,0.12)_0%,rgba(255,255,255,0.025)_50%,rgba(255,255,255,0)_100%)]',
          footer: 'flex-wrap justify-between'
        }"
        @update:open="handleDetailOpenChange"
        @after:enter="revealOrderDetail"
      >
        <!-- Label tombol tutup dibaca dalam bahasa Indonesia. -->
        <template #close="{ ui }">
          <UButton
            icon="i-lucide-x"
            color="neutral"
            variant="ghost"
            :class="ui.close()"
            aria-label="Tutup detail pesanan"
          />
        </template>
        <template #body>
          <div v-if="selected" class="space-y-5 text-sm">
            <!-- Tampilkan foto wahana di detail tiket. -->
            <div class="relative h-40 overflow-hidden rounded-xl sm:h-48">
              <img
                :src="wahanaImage(selected.wahana_id)"
                :alt="`Foto ${selected.wahana_name}`"
                class="size-full object-cover"
              >
            </div>

            <div class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0">
                <h3 class="break-words text-base font-semibold text-highlighted">
                  {{ selected.wahana_name }}
                </h3>
                <p class="mt-1 break-words text-muted">
                  Dipesan oleh {{ selected.username }}
                </p>
              </div>
              <UBadge :color="selected.status === 'Active' ? 'success' : selected.status === 'Used' ? 'primary' : 'neutral'" variant="subtle">
                {{ statusLabel(selected.status) }}
              </UBadge>
            </div>

            <section class="space-y-3 border-t border-default pt-4" aria-label="Kunjungan dan check-in">
              <h3 class="font-semibold text-highlighted">
                Kunjungan & check-in
              </h3>
              <dl class="grid grid-cols-2 gap-4">
                <div>
                  <dt class="text-muted">
                    Tanggal kunjungan
                  </dt>
                  <dd class="mt-1 font-medium text-highlighted">
                    {{ formatDate(selected.visit_date) }}
                  </dd>
                </div>
                <div>
                  <dt class="text-muted">
                    Pengunjung
                  </dt>
                  <dd class="mt-1 font-medium text-highlighted">
                    {{ selected.quantity }} orang
                  </dd>
                </div>
                <div class="col-span-2 flex flex-wrap items-baseline justify-between gap-2">
                  <dt class="text-muted">
                    {{ orderCheckinLabel(selected.checked_in_quantity, selected.quantity) }}
                  </dt>
                  <dd class="font-semibold text-highlighted tabular-nums">
                    {{ selected.checked_in_quantity }} / {{ selected.quantity }} orang masuk
                  </dd>
                </div>
              </dl>
            </section>

            <section class="space-y-3 border-t border-default pt-4" aria-label="Pembayaran pesanan">
              <h3 class="font-semibold text-highlighted">
                Pembayaran
              </h3>
              <dl class="space-y-3">
                <div class="flex flex-wrap justify-between gap-2">
                  <dt class="text-muted">
                    Dipesan pada
                  </dt>
                  <dd class="text-highlighted">
                    {{ formatDate(selected.booking_date, true) }} WIB
                  </dd>
                </div>
                <div class="flex flex-wrap justify-between gap-2">
                  <dt class="text-muted">
                    Harga per tiket
                  </dt>
                  <dd class="text-highlighted tabular-nums">
                    {{ formatRupiah(selected.unit_price) }}
                  </dd>
                </div>
                <div class="flex flex-wrap justify-between gap-2">
                  <dt class="text-muted">
                    Metode pembayaran
                  </dt>
                  <dd class="text-highlighted">
                    {{ selected.payment_method }}
                  </dd>
                </div>
                <div class="flex flex-wrap items-baseline justify-between gap-2 border-t border-default pt-3">
                  <dt class="font-medium text-highlighted">
                    Total pesanan
                  </dt>
                  <dd class="text-lg font-semibold text-highlighted tabular-nums">
                    {{ formatRupiah(selected.total_price) }}
                  </dd>
                </div>
              </dl>
            </section>
          </div>
        </template>
        <template #footer>
          <UButton
            v-if="selected"
            :to="{ path: '/checkins', query: { ticket: selected.id } }"
            label="Lihat riwayat check-in"
            color="neutral"
            variant="outline"
            trailing-icon="i-lucide-arrow-up-right"
          />
          <UButton
            label="Tutup"
            color="neutral"
            variant="subtle"
            @click="handleDetailOpenChange(false)"
          />
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
