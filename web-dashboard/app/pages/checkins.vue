<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import type { DashboardCheckin, DashboardCheckinsApiResponse } from '~/types/tourify'
import { wahanaImages } from '~/utils/wahana-images'

const api = useTourifyApi()
const route = useRoute()
const router = useRouter()
// Pakai ID tiket persis dari pesanan supaya hasil scan tidak tertukar.
const ticketId = computed(() => {
  const ticketQuery = route.query.ticket
  if (typeof ticketQuery !== 'string' || !/^[1-9]\d{0,9}$/.test(ticketQuery)) {
    return ''
  }

  return ticketQuery
})
const rows = ref<DashboardCheckin[]>([])
const search = ref('')
const query = refDebounced(search, 300)
const date = ref('')
const page = ref(1)
const total = ref(0)
const perPage = ref(20)
const summaryStats = ref<DashboardCheckinsApiResponse['stats'] | null>(null)
const loading = ref(true)
const errorMessage = ref('')
// Kartu dan tabel memakai pesan kosong yang sama.
const emptyCheckinsMessage = computed(() => {
  if (search.value || date.value) {
    return 'Tidak ada riwayat sesuai filter.'
  }

  if (ticketId.value) {
    return `Belum ada pengunjung masuk untuk tiket #${ticketId.value}.`
  }

  return 'Riwayat check-in akan tampil setelah tiket dipindai.'
})
let requestId = 0
const table = useTemplateRef('table')
const columnVisibility = ref()
// Tampilkan daftar dulu; kartu ringkas bisa dipilih lewat menu Tampilan.
const viewMode = ref<'compact' | 'list'>('list')

// Pembaruan otomatis tidak mengosongkan tabel atau mengubah filter petugas.
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
      ticket_id: ticketId.value
    })
    const response = await api.get<DashboardCheckinsApiResponse>(`dashboard_checkins.php?${params}`)
    if (request !== requestId) return
    rows.value = response.checkins
    total.value = response.total
    perPage.value = response.per_page
    summaryStats.value = response.stats
    page.value = response.page
    errorMessage.value = ''
  } catch (error) {
    if (request === requestId) errorMessage.value = getTourifyErrorMessage(error, 'Data gagal dimuat. Coba lagi.')
  } finally {
    if (request === requestId) loading.value = false
  }
}

// Mulai dari halaman pertama setelah filter berubah.
watch([query, date, ticketId], () => {
  if (page.value !== 1) page.value = 1
  else loadRows()
})
watch(page, () => loadRows())
onMounted(() => loadRows())
useDashboardAutoRefresh(() => loadRows(true), () => loading.value)

// Tombol reset menghapus semua filter riwayat check-in.
function resetFilters() {
  search.value = ''
  date.value = ''
  void router.replace({ query: { ...route.query, ticket: undefined } })
}

function formatDate(value: string, withTime = false): string {
  const [day, time] = value.split(' ')
  const formatted = (day ?? '').split('-').reverse().join('-')
  return withTime ? `${formatted} ${(time ?? '').slice(0, 8)}` : formatted
}

const numberFormat = new Intl.NumberFormat('id-ID')
const summaryCards = computed(() => [
  {
    label: 'Total check-in',
    value: summaryStats.value ? numberFormat.format(summaryStats.value.checkins) : '—',
    hint: 'Semua catatan masuk',
    icon: 'i-lucide-scan-line'
  },
  {
    label: 'Tiket dengan check-in',
    value: summaryStats.value ? numberFormat.format(summaryStats.value.tickets) : '—',
    hint: 'Tiket berbeda',
    icon: 'i-lucide-ticket-check'
  },
  {
    label: 'Wahana teratas',
    value: summaryStats.value?.top_wahana ?? '—',
    hint: summaryStats.value
      ? summaryStats.value.top_wahana
        ? `${numberFormat.format(summaryStats.value.top_wahana_checkins)} check-in`
        : 'Belum ada check-in'
      : 'Data belum tersedia',
    icon: 'i-lucide-map-pin',
    image: summaryStats.value?.top_wahana_id
      ? wahanaImages[summaryStats.value.top_wahana_id]
      : undefined
  }
])

// Nomor tiket tetap tampil supaya petugas bisa membuka pesanan terkait.
const columns: TableColumn<DashboardCheckin>[] = [
  { accessorKey: 'ticket_id', header: 'Tiket / pelanggan', enableHiding: false, cell: ({ row }) => `#${row.original.ticket_id}` },
  { accessorKey: 'scanned_at', header: 'Waktu masuk (WIB)', cell: ({ row }) => formatDate(row.original.scanned_at, true) },
  { accessorKey: 'wahana_name', header: 'Wahana' },
  { accessorKey: 'staff_username', header: 'Petugas' }
]

// Di layar kecil, tampilkan kolom yang dipilih petugas.
function isCheckinColumnVisible(id: string): boolean {
  return columnVisibility.value?.[id] !== false
}

// Menu Tampilan mengatur kartu, daftar, dan kolom tabel.
const displayItems = computed(() => {
  const items = [
    { label: 'Tampilan riwayat', type: 'label' as const },
    {
      label: 'Ringkas',
      icon: viewMode.value === 'compact' ? 'i-lucide-check' : 'i-lucide-panels-top-left',
      onSelect() {
        viewMode.value = 'compact'
      }
    },
    {
      label: 'Daftar',
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
  <UDashboardPanel id="checkins">
    <template #header>
      <UDashboardNavbar title="Riwayat Check-in">
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

        <!-- Ringkasan ini menjelaskan arti check-in dan tanggalnya. -->
        <p class="text-sm text-muted">
          Satu baris mencatat satu pengunjung masuk. Klik nomor tiket untuk melihat pesanan.
        </p>
        <div class="flex flex-wrap items-end gap-3">
          <UFormField label="Cari riwayat" class="w-full sm:min-w-64 sm:flex-1">
            <UInput
              v-model="search"
              class="w-full"
              icon="i-lucide-search"
              placeholder="Nomor tiket, pelanggan, wahana, atau petugas…"
              aria-label="Cari riwayat check-in"
            />
          </UFormField>
          <UFormField label="Tanggal check-in" class="min-w-40 flex-1 sm:flex-none">
            <UInput
              v-model="date"
              type="date"
              aria-label="Filter tanggal check-in"
              class="w-full"
            />
          </UFormField>
          <div class="flex items-center gap-2">
            <UButton
              v-if="search || date || ticketId"
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

        <div v-if="ticketId" class="flex flex-wrap items-center gap-2 text-sm text-muted">
          <UBadge color="neutral" variant="subtle">
            Tiket #{{ ticketId }}
          </UBadge>
          <UButton
            :to="{ path: '/orders', query: { ticket: ticketId } }"
            label="Lihat pesanan"
            color="neutral"
            variant="link"
            size="sm"
          />
          <UButton
            label="Lihat semua riwayat"
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
        <UTable
          ref="table"
          v-model:column-visibility="columnVisibility"
          :data="rows"
          :columns="columns"
          :loading="loading"
          :class="viewMode === 'list' ? 'hidden shrink-0 xl:block' : 'hidden'"
          :ui="{
            base: 'w-full border-separate border-spacing-0',
            thead: '[&>tr]:bg-elevated/50 [&>tr]:after:content-none',
            tbody: '[&>tr]:last:[&>td]:border-b-0',
            th: 'py-3 first:rounded-l-lg last:rounded-r-lg border-y border-default first:border-l last:border-r first:sticky first:left-0 first:z-20 first:bg-elevated',
            td: 'min-w-36 py-3 border-b border-default align-middle whitespace-normal first:sticky first:left-0 first:z-10 first:bg-default',
            separator: 'h-0'
          }"
        >
          <template #empty>
            {{ emptyCheckinsMessage }}
          </template>
          <!-- Tampilkan waktu scan dan nomor tiket dari riwayat. -->
          <template #scanned_at-cell="{ row }">
            <div>
              <p class="font-medium tabular-nums text-highlighted">
                {{ row.original.scanned_at.split(' ')[1] }} <span class="text-sm font-normal text-muted">WIB</span>
              </p>
              <p class="mt-1 text-sm text-muted">
                {{ formatDate(row.original.scanned_at) }}
              </p>
            </div>
          </template>
          <template #ticket_id-cell="{ row }">
            <NuxtLink :to="{ path: '/orders', query: { ticket: row.original.ticket_id } }" class="rounded font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary" :aria-label="`Lihat pesanan tiket #${row.original.ticket_id} milik ${row.original.username}`">
              #{{ row.original.ticket_id }}
            </NuxtLink>
            <p class="mt-1 break-words text-sm text-muted">
              {{ row.original.username }}
            </p>
          </template>
          <template #staff_username-cell="{ row }">
            <span class="break-words text-highlighted">{{ row.original.staff_username }}</span>
          </template>
        </UTable>

        <!-- Kartu menampilkan waktu, wahana, dan petugas yang memindai. -->
        <div v-if="viewMode === 'compact'" class="space-y-3">
          <p v-if="loading && !rows.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat riwayat check-in…
          </p>
          <p v-else-if="!rows.length" class="py-6 text-center text-sm text-muted">
            {{ emptyCheckinsMessage }}
          </p>
          <article
            v-for="checkin in rows"
            :key="checkin.id"
            class="rounded-xl border border-default bg-default p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <NuxtLink
                  :to="{ path: '/orders', query: { ticket: checkin.ticket_id } }"
                  class="inline-flex min-h-11 items-center rounded font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                  :aria-label="`Lihat pesanan tiket #${checkin.ticket_id} milik ${checkin.username}`"
                >
                  Tiket #{{ checkin.ticket_id }}
                </NuxtLink>
                <p class="break-words text-sm text-muted">
                  {{ checkin.username }}
                </p>
              </div>
              <div class="shrink-0 text-right">
                <p class="font-medium tabular-nums text-highlighted">
                  {{ checkin.scanned_at.split(' ')[1]?.slice(0, 8) }} WIB
                </p>
                <p class="mt-1 text-sm text-muted">
                  {{ formatDate(checkin.scanned_at) }}
                </p>
              </div>
            </div>
            <dl class="mt-3 grid grid-cols-2 gap-3 border-t border-default pt-3 text-sm">
              <div class="min-w-0">
                <dt class="text-muted">
                  Wahana
                </dt>
                <dd class="mt-1 break-words font-medium text-highlighted">
                  {{ checkin.wahana_name }}
                </dd>
              </div>
              <div class="min-w-0">
                <dt class="text-muted">
                  Petugas
                </dt>
                <dd class="mt-1 break-words text-highlighted">
                  {{ checkin.staff_username }}
                </dd>
              </div>
            </dl>
          </article>
        </div>

        <!-- Di layar kecil, tiap check-in tampil sebagai satu baris ringkas. -->
        <section
          v-if="viewMode === 'list'"
          aria-label="Daftar riwayat check-in"
          class="divide-y divide-default border-y border-default xl:hidden"
        >
          <p v-if="loading && !rows.length" role="status" class="py-6 text-center text-sm text-muted">
            Memuat riwayat check-in…
          </p>
          <p v-else-if="!rows.length" class="py-6 text-center text-sm text-muted">
            {{ emptyCheckinsMessage }}
          </p>
          <article
            v-for="checkin in rows"
            :key="checkin.id"
            class="flex flex-wrap items-center gap-x-5 gap-y-3 px-1 py-3 sm:px-3"
          >
            <div class="min-w-36 flex-1">
              <NuxtLink
                :to="{ path: '/orders', query: { ticket: checkin.ticket_id } }"
                class="inline-flex min-h-11 items-center rounded font-semibold text-primary underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary"
                :aria-label="`Lihat pesanan tiket #${checkin.ticket_id} milik ${checkin.username}`"
              >
                Tiket #{{ checkin.ticket_id }}
              </NuxtLink>
              <p class="break-words text-sm text-muted">
                {{ checkin.username }}
              </p>
            </div>

            <dl class="grid w-full grid-cols-2 gap-x-4 gap-y-2 text-sm sm:w-auto sm:flex sm:flex-wrap sm:gap-x-5">
              <div v-if="isCheckinColumnVisible('scanned_at')">
                <dt class="text-muted">
                  Waktu masuk
                </dt>
                <dd class="mt-1 tabular-nums text-highlighted">
                  {{ checkin.scanned_at.split(' ')[1]?.slice(0, 8) }} WIB
                  <span class="ml-2 text-muted">{{ formatDate(checkin.scanned_at) }}</span>
                </dd>
              </div>
              <div v-if="isCheckinColumnVisible('wahana_name')">
                <dt class="text-muted">
                  Wahana
                </dt>
                <dd class="mt-1 break-words text-highlighted">
                  {{ checkin.wahana_name }}
                </dd>
              </div>
              <div v-if="isCheckinColumnVisible('staff_username')">
                <dt class="text-muted">
                  Petugas
                </dt>
                <dd class="mt-1 break-words text-highlighted">
                  {{ checkin.staff_username }}
                </dd>
              </div>
            </dl>
          </article>
        </section>

        <div class="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-default pt-4">
          <p class="text-sm text-muted">
            {{ total }} check-in · {{ perPage }} per halaman
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
    </template>
  </UDashboardPanel>
</template>
