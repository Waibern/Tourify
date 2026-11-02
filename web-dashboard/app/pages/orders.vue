<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import type { DashboardOrder, DashboardOrdersApiResponse } from '~/types/tourify'
import { wahanaImage } from '~/utils/wahana-images'
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'

const api = useTourifyApi()
const route = useRoute()
const router = useRouter()
// Tautan dari riwayat membatasi hasil ke satu tiket, bukan pencarian ID yang mirip.
const ticketId = computed(() => typeof route.query.ticket === 'string' && /^[1-9]\d{0,9}$/.test(route.query.ticket) ? route.query.ticket : '')
let openedTicketId = ''
const rows = ref<DashboardOrder[]>([])
const search = ref('')
const query = refDebounced(search, 300)
const date = ref('')
const page = ref(1)
const total = ref(0)
const perPage = ref(20)
const summaryStats = ref<DashboardOrdersApiResponse['stats'] | null>(null)
const loading = ref(true)
const errorMessage = ref('')
let requestId = 0
const status = ref('all')
const selected = ref<DashboardOrder | null>(null)
const detailOpen = ref(false)
const reducedMotion = usePreferredReducedMotion()
const detailSurfaceSelector = '[data-slot="content"][role="dialog"]'
const table = useTemplateRef('table')
const columnVisibility = ref()
let detailReveal: AsciiRevealController | null = null
let closingDetail = false
const statusItems = [
  { label: 'Semua status', value: 'all' },
  { label: 'Aktif', value: 'Active' },
  { label: 'Terpakai', value: 'Used' },
  { label: 'Kedaluwarsa', value: 'Expire' }
]

// Detail memakai pesanan dari tabel yang sedang ditampilkan.
function showDetail(order: DashboardOrder) {
  selected.value = order
  detailOpen.value = true
}

// Detail bergerak dengan ASCII saat dibuka dan menyelesaikan efeknya sebelum ditutup.
async function revealOrderDetail() {
  if (reducedMotion.value === 'reduce') return

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
    // Detail tetap terbaca kalau efek ASCII gagal.
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
    if (controller) await controller.hide(surface ? { within: surface } : {})
  } catch {
    // Modal tetap ditutup kalau efek ASCII gagal.
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

// Controller dilepas kalau halaman pesanan ditinggalkan saat dialog masih terbuka.
onBeforeUnmount(() => {
  detailReveal?.destroy()
  detailReveal = null
})

// Reset menghapus semua pembatas, termasuk tiket yang dibuka dari halaman riwayat.
function resetFilters() {
  search.value = ''
  date.value = ''
  status.value = 'all'
  void router.replace({ query: { ...route.query, ticket: undefined } })
}

// Polling tidak menyalakan loading; respons lama tetap diabaikan saat filter berubah.
async function loadRows(background = false) {
  const request = ++requestId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const params = new URLSearchParams({ q: query.value.trim(), date: date.value, page: String(page.value), status: status.value === 'all' ? '' : status.value, ticket_id: ticketId.value })
    const response = await api.get<DashboardOrdersApiResponse>(`dashboard_orders.php?${params}`)
    if (request !== requestId) return
    rows.value = response.orders
    total.value = response.total
    perPage.value = response.per_page
    summaryStats.value = response.stats
    page.value = response.page
    errorMessage.value = ''
    // Detail terbuka ikut mendapat status check-in terbaru dari pesanan yang sama.
    if (detailOpen.value && selected.value) {
      selected.value = response.orders.find(order => order.id === selected.value?.id) ?? selected.value
    }
    // Detail tautan tiket dibuka sekali; menutup modal tidak memunculkannya lagi saat refresh data.
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

// Filter baru dimulai dari halaman pertama agar hasil tidak terlewat.
watch(ticketId, () => {
  openedTicketId = ''
})
watch([query, date, status, ticketId], () => {
  if (page.value !== 1) page.value = 1
  else loadRows()
})
watch(page, () => loadRows())
onMounted(() => loadRows())
useDashboardAutoRefresh(() => loadRows(true), () => loading.value)

function formatDate(value: string, withTime = false): string {
  const [day, time] = value.split(' ')
  const formatted = (day ?? '').split('-').reverse().join('-')
  return withTime ? `${formatted} ${(time ?? '').slice(0, 8)}` : formatted
}
// Nilai uang diformat dari total tersimpan, bukan dihitung ulang dari harga wahana terbaru.
function formatRupiah(value: number): string {
  return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(value)
}

const numberFormat = new Intl.NumberFormat('id-ID')
const summaryCards = computed(() => [
  {
    label: 'Total pesanan',
    value: summaryStats.value ? numberFormat.format(summaryStats.value.orders) : '—',
    hint: 'Semua pesanan',
    icon: 'i-lucide-receipt'
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

const columns: TableColumn<DashboardOrder>[] = [
  { accessorKey: 'id', header: 'Tiket / pelanggan', enableHiding: false, cell: ({ row }) => `#${row.original.id}` },
  { accessorKey: 'wahana_name', header: 'Wahana' },
  { accessorKey: 'visit_date', header: 'Kunjungan', cell: ({ row }) => formatDate(row.original.visit_date) },
  { accessorKey: 'total_price', header: 'Total', cell: ({ row }) => formatRupiah(row.original.total_price) },
  { accessorKey: 'checked_in_quantity', header: 'Check-in' },
  { accessorKey: 'status', header: 'Status tiket' },
  { id: 'actions', header: 'Detail', enableHiding: false }
]

// Menu tampilan memakai kolom yang tersedia di tabel pesanan.
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

        <!-- Label tetap terlihat supaya tanggal kunjungan tidak tertukar dengan tanggal pemesanan. -->
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

        <UAlert
          v-if="errorMessage"
          color="error"
          variant="subtle"
          :description="errorMessage"
          :actions="[{ label: 'Coba lagi', onClick: () => loadRows() }]"
        />
        <!-- Nomor tiket tetap terlihat saat tabel digeser dan bisa langsung membuka detail. -->
        <UTable
          ref="table"
          v-model:column-visibility="columnVisibility"
          :data="rows"
          :columns="columns"
          :loading="loading"
          class="hidden shrink-0 xl:block"
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
            {{ search || date || status !== 'all' ? 'Tidak ada pesanan sesuai filter.' : ticketId ? `Tiket #${ticketId} tidak ditemukan.` : 'Pesanan akan tampil setelah tiket dipesan.' }}
          </template>
          <!-- Detail di tiap baris tetap memakai data tiket Tourify. -->
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

        <!-- Ringkas tiap tiket supaya data dan aksi utama muat di layar sentuh. -->
        <div class="space-y-3 xl:hidden">
          <p v-if="loading && !rows.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat pesanan…
          </p>
          <p v-else-if="!rows.length" class="py-6 text-center text-sm text-muted">
            {{ search || date || status !== 'all' ? 'Tidak ada pesanan sesuai filter.' : ticketId ? `Tiket #${ticketId} tidak ditemukan.` : 'Pesanan akan tampil setelah tiket dipesan.' }}
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
      <!-- Detail punya gradasi tipis dan reveal ASCII saat buka-tutup. -->
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
        <!-- Nama tombol tutup tetap bahasa Indonesia untuk pembaca layar. -->
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
