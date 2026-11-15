<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { DashboardApiResponse, TourifyWahana, WahanaUpdateApiResponse } from '~/types/tourify'
import { wahanaImage } from '~/utils/wahana-images'

const api = useTourifyApi()
const { notifySuccess } = useDashboardNotifications()
const wahana = ref<TourifyWahana[]>([])
const search = ref('')
const sort = ref('name')
// Tampilkan daftar dulu; kartu foto bisa dipilih lewat menu Tampilan.
const view = ref<'cards' | 'list'>('list')
const loading = ref(true)
const saving = ref(false)
const editorOpen = ref(false)
const errorMessage = ref('')
const editorErrorMessage = ref('')
const selectedWahanaId = ref<number | null>(null)
let latestLoadId = 0
const pagination = ref({ pageIndex: 0, pageSize: 10 })
// Batasi lebar kolom supaya foto tidak memenuhi layar.
const wahanaGridStyle = {
  gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 16rem), 1fr))'
}
watch([search, sort], () => {
  pagination.value.pageIndex = 0
})
const editorState = reactive({
  name: '',
  description: '',
  ticket_price: 1
})

const schema = z.object({
  name: z.string().trim().min(1, 'Nama wahana wajib diisi.').max(100, 'Nama maksimal 100 karakter.'),
  description: z.string().max(10000, 'Deskripsi maksimal 10.000 karakter.'),
  ticket_price: z.number().int().min(1, 'Harga harus lebih dari nol.').max(4294967295, 'Harga terlalu besar.')
})

type WahanaForm = z.output<typeof schema>

const filteredWahana = computed(() => {
  const query = search.value.trim().toLocaleLowerCase('id-ID')
  const rows = wahana.value.filter(item =>
    !query
    || item.name.toLocaleLowerCase('id-ID').includes(query)
    || item.description.toLocaleLowerCase('id-ID').includes(query)
  )

  return rows.sort((a, b) => {
    if (sort.value === 'price-low') return a.ticket_price - b.ticket_price
    if (sort.value === 'price-high') return b.ticket_price - a.ticket_price
    return a.name.localeCompare(b.name, 'id-ID')
  })
})

const visibleWahana = computed(() => {
  const start = pagination.value.pageIndex * pagination.value.pageSize
  return filteredWahana.value.slice(start, start + pagination.value.pageSize)
})

function formatRupiah(amount: number): string {
  return new Intl.NumberFormat('id-ID', {
    style: 'currency',
    currency: 'IDR',
    maximumFractionDigits: 0
  }).format(amount)
}

// Pembaruan otomatis tidak mengulang animasi atau mengganti daftar saat form dibuka.
async function loadWahana(background = false) {
  const loadId = ++latestLoadId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const response = await api.get<DashboardApiResponse>('dashboard_wahana.php')
    if (loadId !== latestLoadId || (background && (editorOpen.value || saving.value))) return
    if (JSON.stringify(wahana.value) !== JSON.stringify(response.wahana ?? [])) {
      wahana.value = response.wahana ?? []
    }
    errorMessage.value = ''
  } catch (error) {
    if (loadId === latestLoadId) errorMessage.value = getTourifyErrorMessage(error, 'Daftar wahana gagal dimuat.')
  } finally {
    if (loadId === latestLoadId) loading.value = false
  }
}

function openEditor(item: TourifyWahana) {
  editorErrorMessage.value = ''
  selectedWahanaId.value = item.id
  editorState.name = item.name
  editorState.description = item.description
  editorState.ticket_price = item.ticket_price
  editorOpen.value = true
}

// Perubahan harga wahana tidak mengubah harga tiket yang sudah dibeli.
async function saveWahana(event: FormSubmitEvent<WahanaForm>) {
  if (selectedWahanaId.value === null) {
    return
  }

  saving.value = true
  errorMessage.value = ''
  editorErrorMessage.value = ''
  try {
    const response = await api.put<WahanaUpdateApiResponse>('dashboard_wahana.php', {
      id: selectedWahanaId.value,
      ...event.data
    })
    if (!response.wahana) {
      throw new Error(response.message || 'Data wahana tidak diterima dari server.')
    }

    // Abaikan hasil pemuatan lama supaya harga baru tidak tertimpa.
    latestLoadId++
    loading.value = false
    wahana.value = wahana.value.map(item =>
      item.id === response.wahana?.id ? response.wahana : item
    )
    editorOpen.value = false
    notifySuccess('Perubahan tersimpan', response.message || response.wahana.name)
  } catch (error) {
    editorErrorMessage.value = getTourifyErrorMessage(error, 'Perubahan wahana gagal disimpan.')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadWahana())
useDashboardAutoRefresh(() => loadWahana(true), () => loading.value || saving.value || editorOpen.value)
</script>

<template>
  <UDashboardPanel id="wahana">
    <template #header>
      <UDashboardNavbar title="Wahana">
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
            @click="loadWahana()"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div class="flex min-h-0 flex-1 flex-col gap-4">
        <div class="flex flex-wrap items-end gap-3">
          <UFormField label="Cari wahana" class="w-full sm:min-w-64 sm:flex-1">
            <UInput
              v-model="search"
              class="w-full"
              icon="i-lucide-search"
              placeholder="Nama atau deskripsi wahana…"
              aria-label="Cari wahana"
            />
          </UFormField>
          <UFormField label="Urutkan" class="flex-1 sm:flex-none">
            <USelect
              v-model="sort"
              :items="[
                { label: 'Nama A–Z', value: 'name' },
                { label: 'Harga terendah', value: 'price-low' },
                { label: 'Harga tertinggi', value: 'price-high' }
              ]"
              aria-label="Urutkan wahana"
              class="w-full min-w-40"
            />
          </UFormField>
          <UButton
            v-if="search || sort !== 'name'"
            label="Reset"
            color="neutral"
            variant="ghost"
            @click="search = ''; sort = 'name'"
          />
          <!-- Tombol pilihan tampilan memberi tahu pembaca layar mana yang sedang aktif. -->
          <UFieldGroup aria-label="Tampilan wahana" role="group">
            <UButton
              label="Kartu"
              icon="i-lucide-layout-grid"
              color="neutral"
              :variant="view === 'cards' ? 'subtle' : 'outline'"
              :aria-pressed="view === 'cards'"
              @click="view = 'cards'"
            />
            <UButton
              label="Daftar ringkas"
              icon="i-lucide-list"
              color="neutral"
              :variant="view === 'list' ? 'subtle' : 'outline'"
              :aria-pressed="view === 'list'"
              @click="view = 'list'"
            />
          </UFieldGroup>
        </div>

        <UAlert
          v-if="errorMessage"
          color="error"
          variant="subtle"
          :description="errorMessage"
          :actions="[{ label: 'Coba lagi', onClick: () => loadWahana() }]"
        />

        <!-- Kartu tidak melebar berlebihan di layar besar. -->
        <!-- Sisakan ruang saat memuat supaya daftar tidak bergeser. -->
        <AsciiReveal
          :loading="loading"
          duration="fast"
          :speed="1.2"
          class="min-w-0"
        >
          <template #skeleton>
            <div class="min-h-80" role="status" aria-label="Memuat daftar wahana" />
          </template>

          <template #default>
            <div
              v-if="!filteredWahana.length"
              class="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-dashed border-default px-6 text-center"
            >
              <UIcon name="i-lucide-search" class="size-6 text-muted" />
              <p class="mt-3 font-medium text-highlighted">
                {{ search ? 'Wahana tidak ditemukan.' : 'Belum ada data wahana.' }}
              </p>
              <UButton
                v-if="search"
                label="Hapus pencarian"
                color="neutral"
                variant="link"
                class="mt-2"
                @click="search = ''"
              />
            </div>

            <!-- Kartu memakai foto wahana dan animasi yang sama dengan Ringkasan. -->
            <div
              v-else
              class="gap-3"
              :class="view === 'cards' ? 'grid' : 'flex flex-col'"
              :style="view === 'cards' ? wahanaGridStyle : undefined"
            >
              <article
                v-for="item in visibleWahana"
                :key="item.id"
                data-wahana-card
                class="group flex rounded-2xl border border-default bg-default p-3 transition-colors hover:border-white/25 motion-reduce:transition-none"
                :class="view === 'cards' ? 'h-full flex-col' : 'items-center gap-3 sm:gap-4'"
              >
                <div class="overflow-hidden rounded-xl" :class="view === 'cards' ? 'w-full' : 'w-14 shrink-0 sm:w-16'">
                  <img
                    :src="wahanaImage(item.id)"
                    :alt="`Foto ${item.name}`"
                    loading="lazy"
                    class="w-full object-cover transition-transform duration-500 group-hover:scale-105 motion-reduce:transition-none"
                    :class="view === 'cards' ? 'aspect-[16/10]' : 'aspect-square'"
                  >
                </div>
                <div class="min-w-0 flex-1" :class="view === 'cards' ? 'mt-3' : ''">
                  <div class="flex flex-col gap-1" :class="view === 'list' ? 'sm:flex-row sm:items-baseline sm:justify-between sm:gap-4' : ''">
                    <h2 class="break-words text-base font-semibold text-highlighted" :class="view === 'cards' ? 'line-clamp-2' : ''">
                      {{ item.name }}
                    </h2>
                    <p class="shrink-0 font-semibold text-highlighted tabular-nums">
                      {{ formatRupiah(item.ticket_price) }} <span class="text-sm font-normal text-muted">/ tiket</span>
                    </p>
                  </div>
                  <p class="mt-2 text-sm text-muted" :class="view === 'cards' ? 'line-clamp-2 min-h-10' : 'line-clamp-1'">
                    {{ item.description }}
                  </p>
                </div>
                <!-- Lapisan transparan menjaga tombol tetap terlihat di atas foto. -->
                <UButton
                  label="Edit"
                  color="neutral"
                  variant="ghost"
                  size="sm"
                  class="h-11 shrink-0 justify-center rounded-full border border-neutral-300/60 bg-white/45 font-medium text-neutral-900 shadow-[inset_0_1px_0_rgb(255_255_255/65%),0_3px_10px_rgb(0_0_0/6%)] backdrop-blur-xl transition-colors hover:bg-white/65 active:bg-white/80 dark:border-white/20 dark:bg-white/10 dark:text-white dark:shadow-[inset_0_1px_0_rgb(255_255_255/20%),0_3px_10px_rgb(0_0_0/15%)] dark:hover:bg-white/16 dark:active:bg-white/22 motion-reduce:transition-none"
                  :class="view === 'cards' ? 'mt-3 w-full' : 'px-4'"
                  :aria-label="`Edit ${item.name}`"
                  @click="openEditor(item)"
                />
              </article>
            </div>
          </template>
        </AsciiReveal>

        <div class="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-default pt-4">
          <p class="text-sm text-muted">
            {{ loading ? 'Memuat wahana…' : `${filteredWahana.length} wahana` }}
          </p>
          <UPagination
            v-if="filteredWahana.length > pagination.pageSize"
            :page="pagination.pageIndex + 1"
            :items-per-page="pagination.pageSize"
            :total="filteredWahana.length"
            @update:page="(page: number) => pagination.pageIndex = page - 1"
          />
        </div>
      </div>

      <UModal
        v-model:open="editorOpen"
        title="Edit wahana"
        description="Perubahan harga berlaku untuk pemesanan berikutnya."
      >
        <!-- Tombol tutup memakai label Indonesia yang sama dengan dialog lain. -->
        <template #close="{ ui }">
          <UButton
            icon="i-lucide-x"
            color="neutral"
            variant="ghost"
            :class="ui.close()"
            aria-label="Tutup editor wahana"
          />
        </template>
        <template #body>
          <UAlert
            v-if="editorErrorMessage"
            color="error"
            variant="subtle"
            :description="editorErrorMessage"
            class="mb-4"
          />

          <UForm
            :schema="schema"
            :state="editorState"
            class="space-y-4"
            @submit="saveWahana"
          >
            <UFormField label="Nama wahana" name="name">
              <UInput v-model="editorState.name" maxlength="100" class="w-full" />
            </UFormField>
            <UFormField label="Deskripsi" name="description">
              <UTextarea v-model="editorState.description" :rows="4" class="w-full" />
            </UFormField>
            <UFormField label="Harga tiket (rupiah)" name="ticket_price">
              <!-- Harga boleh berupa angka rupiah bulat mulai dari Rp1. -->
              <UInput
                v-model.number="editorState.ticket_price"
                type="number"
                min="1"
                step="1"
                class="w-full"
              />
            </UFormField>
            <div class="flex justify-end gap-2">
              <UButton
                label="Batal"
                color="neutral"
                variant="subtle"
                @click="editorOpen = false"
              />
              <UButton
                type="submit"
                label="Simpan perubahan"
                :loading="saving"
              />
            </div>
          </UForm>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
