<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { DashboardApiResponse, TourifyWahana, WahanaUpdateApiResponse } from '~/types/tourify'
import { wahanaImages } from '~/utils/wahana-images'

const api = useTourifyApi()
const { notifySuccess } = useDashboardNotifications()
const wahana = ref<TourifyWahana[]>([])
const search = ref('')
const sort = ref('name')
// Tampilkan daftar dulu; kartu foto bisa dipilih lewat menu Tampilan.
const view = ref<'cards' | 'list'>('list')
const loading = ref(true)
const saving = ref(false)
// Hindari perubahan unggulan bertumpuk saat permintaan masih berjalan.
const savingFeaturedId = ref<number | null>(null)
const deleteModalOpen = ref(false)
const deleting = ref(false)
const deleteErrorMessage = ref('')
const pendingDeleteWahana = shallowRef<TourifyWahana | null>(null)
const editorOpen = ref(false)
const errorMessage = ref('')
const editorErrorMessage = ref('')
const selectedWahanaId = ref<number | null>(null)
const photoInput = ref<HTMLInputElement | null>(null)
const selectedPhoto = shallowRef<File | null>(null)
const previewUrl = ref<string | null>(null)
const photoError = ref('')
const failedImages = reactive(new Set<string>())

// Gunakan foto bawaan hanya untuk wahana yang memang punya aset lokal.
function photoUrl(item: TourifyWahana): string | undefined {
  const uploadUrl = api.assetUrl(item.image_path)
  return uploadUrl && !failedImages.has(uploadUrl) ? uploadUrl : wahanaImages[item.id]
}

function handlePreviewError(event: Event) {
  if (previewUrl.value) {
    photoError.value = 'Foto tidak bisa dibuka. Pilih foto lain atau batalkan pilihan.'
    return
  }
  markPhotoFailed(event)
}

function markPhotoFailed(event: Event) {
  const source = (event.target as HTMLImageElement).getAttribute('src')
  if (source) failedImages.add(source)
}

const editorPhotoUrl = computed(() => {
  const item = wahana.value.find(row => row.id === selectedWahanaId.value)
  return previewUrl.value ?? (item ? photoUrl(item) : undefined)
})

function clearSelectedPhoto() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  selectedPhoto.value = null
  photoError.value = ''
  if (photoInput.value) photoInput.value.value = ''
}

function selectPhoto(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  selectedPhoto.value = null
  photoError.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    photoError.value = 'Pilih foto JPG, PNG, atau WebP.'
  } else if (file.size < 1 || file.size > 5 * 1024 * 1024) {
    photoError.value = 'Foto harus berisi data dan berukuran maksimal 5 MB.'
  } else {
    selectedPhoto.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

watch(editorOpen, (open) => {
  if (!open) clearSelectedPhoto()
})
onBeforeUnmount(clearSelectedPhoto)
let latestLoadId = 0
const pagination = ref({ pageIndex: 0, pageSize: 10 })
// Batasi lebar kolom supaya foto tidak memenuhi layar.
const wahanaGridStyle = {
  gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 16rem), 1fr))'
}
watch([search, sort], () => {
  pagination.value.pageIndex = 0
})
// Harga awal hanya dipakai untuk wahana baru.
const defaultTicketPrice = 10000
const editorState = reactive({
  name: '',
  description: '',
  ticket_price: defaultTicketPrice
})

const schema = z.object({
  name: z.string().trim().min(1, 'Nama wahana wajib diisi.').max(100, 'Nama maksimal 100 karakter.'),
  description: z.string().max(10000, 'Deskripsi maksimal 10.000 karakter.'),
  ticket_price: z.number().int().min(1, 'Harga harus lebih dari nol.').max(107374182, 'Harga maksimal Rp107.374.182.')
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

const ratingFormat = new Intl.NumberFormat('id-ID', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1
})

// Pembaruan otomatis tidak mengulang animasi atau mengganti daftar saat form dibuka.
async function loadWahana(background = false) {
  const loadId = ++latestLoadId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
  }
  try {
    const response = await api.get<DashboardApiResponse>('dashboard_wahana.php')
    if (loadId !== latestLoadId || (background && (editorOpen.value || saving.value || savingFeaturedId.value !== null || deleteModalOpen.value || deleting.value))) return
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

// Pilihan baru menggantikan unggulan lama setelah server berhasil menyimpan.
async function setFeatured(item: TourifyWahana) {
  if (savingFeaturedId.value !== null || deleting.value || item.is_featured) return
  savingFeaturedId.value = item.id
  latestLoadId++
  errorMessage.value = ''
  try {
    const response = await api.post<DashboardApiResponse>('dashboard_wahana.php', { id: item.id })
    if (!response.success) throw new Error(response.message || 'Wahana unggulan gagal disimpan.')
    latestLoadId++
    wahana.value = wahana.value.map(row => ({ ...row, is_featured: row.id === item.id }))
    notifySuccess('Wahana unggulan diperbarui', `${item.name} tampil di Beranda aplikasi setelah katalog dimuat ulang.`)
  } catch (error) {
    errorMessage.value = getTourifyErrorMessage(error, 'Wahana unggulan gagal disimpan.')
  } finally {
    savingFeaturedId.value = null
  }
}

function requestDelete(item: TourifyWahana) {
  pendingDeleteWahana.value = item
  deleteErrorMessage.value = ''
  deleteModalOpen.value = true
}

// Ambil wahana yang sedang diedit untuk tombol hapus di formulir.
function requestDeleteEditingWahana() {
  const item = wahana.value.find(row => row.id === selectedWahanaId.value)
  if (item) requestDelete(item)
}

async function deleteWahana() {
  const item = pendingDeleteWahana.value
  if (!item || deleting.value) return

  deleting.value = true
  deleteErrorMessage.value = ''
  latestLoadId++
  loading.value = false
  try {
    const response = await api.delete<DashboardApiResponse>(`dashboard_wahana.php?id=${item.id}`)
    if (!response.success) throw new Error(response.message || 'Wahana gagal dihapus.')

    wahana.value = wahana.value.filter(row => row.id !== item.id)
    const lastPage = Math.max(0, Math.ceil(filteredWahana.value.length / pagination.value.pageSize) - 1)
    pagination.value.pageIndex = Math.min(pagination.value.pageIndex, lastPage)
    editorOpen.value = false
    selectedWahanaId.value = null
    deleteModalOpen.value = false
    pendingDeleteWahana.value = null
    notifySuccess('Wahana dihapus dari katalog', item.name)
  } catch (error) {
    deleteErrorMessage.value = getTourifyErrorMessage(error, 'Wahana gagal dihapus. Coba lagi.')
  } finally {
    deleting.value = false
  }
}

watch(deleteModalOpen, (open) => {
  if (!open && !deleting.value) {
    pendingDeleteWahana.value = null
    deleteErrorMessage.value = ''
  }
})

function openEditor(item?: TourifyWahana) {
  clearSelectedPhoto()
  editorErrorMessage.value = ''
  selectedWahanaId.value = item?.id ?? null
  editorState.name = item?.name ?? ''
  editorState.description = item?.description ?? ''
  editorState.ticket_price = item?.ticket_price ?? defaultTicketPrice
  editorOpen.value = true
}

// Data dan foto disimpan dalam satu permintaan.
async function saveWahana(event: FormSubmitEvent<WahanaForm>) {
  if (saving.value || photoError.value) return
  const id = selectedWahanaId.value
  const creating = id === null
  saving.value = true
  editorErrorMessage.value = ''
  try {
    const response = await api.upload<WahanaUpdateApiResponse>(
      `dashboard_wahana.php?action=${creating ? 'create' : 'update'}`,
      selectedPhoto.value,
      {
        ...(id === null ? {} : { id: String(id) }),
        name: event.data.name,
        description: event.data.description,
        ticket_price: String(event.data.ticket_price)
      }
    )
    const saved = response.wahana
    if (!saved) {
      throw new Error(response.message || 'Data wahana tidak diterima dari server.')
    }

    // Abaikan respons refresh yang dimulai sebelum data disimpan.
    latestLoadId++
    loading.value = false
    if (creating) {
      wahana.value = [...wahana.value, saved]
      search.value = ''
      await nextTick()
      const index = filteredWahana.value.findIndex(item => item.id === saved.id)
      pagination.value.pageIndex = Math.floor(index / pagination.value.pageSize)
    } else {
      wahana.value = wahana.value.map(item => item.id === saved.id ? saved : item)
    }
    editorOpen.value = false
    notifySuccess(creating ? 'Wahana ditambahkan' : 'Perubahan tersimpan', saved.name)
  } catch (error) {
    editorErrorMessage.value = getTourifyErrorMessage(error, 'Wahana gagal disimpan.')
  } finally {
    saving.value = false
  }
}

onMounted(() => loadWahana())
useDashboardAutoRefresh(() => loadWahana(true), () => loading.value || saving.value || editorOpen.value || savingFeaturedId.value !== null || deleteModalOpen.value || deleting.value)
</script>

<template>
  <UDashboardPanel id="wahana">
    <template #header>
      <UDashboardNavbar title="Wahana">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <UButton label="Tambah wahana" icon="i-lucide-plus" :disabled="savingFeaturedId !== null || deleting" @click="openEditor()" />
          <UButton
            label="Perbarui"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            :loading="loading"
            :disabled="deleting"
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
                :class="view === 'cards' ? 'h-full flex-col' : 'flex-wrap items-center gap-3 sm:flex-nowrap sm:gap-4'"
              >
                <div class="overflow-hidden rounded-xl" :class="view === 'cards' ? 'w-full' : 'w-14 shrink-0 sm:w-16'">
                  <img
                    v-if="photoUrl(item)"
                    :src="photoUrl(item)"
                    @error="markPhotoFailed"
                    :alt="`Foto ${item.name}`"
                    loading="lazy"
                    class="w-full object-cover transition-transform duration-500 group-hover:scale-105 motion-reduce:transition-none"
                    :class="view === 'cards' ? 'aspect-[16/10]' : 'aspect-square'"
                  >
                  <div
                    v-else
                    class="grid w-full place-items-center bg-muted text-muted"
                    :class="view === 'cards' ? 'aspect-[16/10]' : 'aspect-square'"
                    aria-hidden="true"
                  >
                    <UIcon name="i-lucide-image" class="size-6" />
                  </div>
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
                  <!-- Rating tampil di kedua mode supaya kualitas wahana cepat terbaca. -->
                  <div
                    class="mt-2 flex flex-wrap items-center gap-1.5 text-sm"
                    :aria-label="item.average_rating === null ? 'Belum ada rating untuk ' + item.name : 'Rating ' + ratingFormat.format(item.average_rating) + ' dari 5, berdasarkan ' + item.rating_count + ' ulasan untuk ' + item.name"
                  >
                    <UIcon
                      name="i-lucide-star"
                      :class="item.rating_count > 0 ? 'fill-amber-400 text-amber-400' : 'text-muted'"
                      class="size-4 shrink-0"
                      aria-hidden="true"
                    />
                    <span class="font-semibold tabular-nums text-highlighted">
                      {{ item.average_rating === null ? 'Belum ada rating' : ratingFormat.format(item.average_rating) }}
                    </span>
                    <span v-if="item.rating_count > 0" class="text-muted">
                      · {{ item.rating_count }} ulasan
                    </span>
                  </div>
                </div>
                <!-- Tombol bintang tersedia di tampilan daftar maupun kartu. -->
                <UButton
                  :label="item.is_featured ? 'Unggulan' : 'Jadikan unggulan'"
                  icon="i-lucide-star"
                  :color="item.is_featured ? 'primary' : 'neutral'"
                  :variant="item.is_featured ? 'soft' : 'ghost'"
                  size="sm"
                  class="min-h-11 shrink-0 justify-center"
                  :class="view === 'cards' ? 'mt-3 w-full' : ''"
                  :loading="savingFeaturedId === item.id"
                  :disabled="saving || savingFeaturedId !== null || item.is_featured"
                  :aria-label="`${item.is_featured ? 'Wahana unggulan:' : 'Jadikan unggulan:'} ${item.name}`"
                  @click="setFeatured(item)"
                />
                <!-- Lapisan transparan menjaga tombol tetap terlihat di atas foto. -->
                <UButton
                  label="Edit"
                  color="neutral"
                  variant="ghost"
                  size="sm"
                  class="h-11 shrink-0 justify-center rounded-full border border-neutral-300/60 bg-white/45 font-medium text-neutral-900 shadow-[inset_0_1px_0_rgb(255_255_255/65%),0_3px_10px_rgb(0_0_0/6%)] backdrop-blur-xl transition-colors hover:bg-white/65 active:bg-white/80 dark:border-white/20 dark:bg-white/10 dark:text-white dark:shadow-[inset_0_1px_0_rgb(255_255_255/20%),0_3px_10px_rgb(0_0_0/15%)] dark:hover:bg-white/16 dark:active:bg-white/22 motion-reduce:transition-none"
                  :class="view === 'cards' ? 'mt-3 w-full' : 'px-4'"
                  :aria-label="`Edit ${item.name}`"
                  :disabled="savingFeaturedId !== null || deleting"
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
        :title="selectedWahanaId === null ? 'Tambah wahana' : 'Edit wahana'"
        :description="selectedWahanaId === null ? 'Isi data wahana untuk menambahkannya ke katalog.' : 'Perubahan harga berlaku untuk pemesanan berikutnya.'"
        :dismissible="!saving"
        :close="!saving"
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
            <!-- Pratinjau lokal menggantikan foto lama sampai admin menyimpan. -->
            <UFormField
              label="Foto wahana (opsional)"
              :error="photoError || undefined"
            >
              <input
                ref="photoInput"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                hidden
                :disabled="saving"
                @change="selectPhoto"
              >
              <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-3 rounded-xl border border-default bg-elevated/50 p-3">
                <div class="flex min-w-0 flex-1 basis-48 items-center gap-3">
                  <div class="grid size-14 shrink-0 place-items-center overflow-hidden rounded-lg bg-muted">
                    <img
                      v-if="editorPhotoUrl"
                      :src="editorPhotoUrl"
                      alt="Pratinjau foto wahana"
                      class="size-full object-cover"
                      @error="handlePreviewError"
                    >
                    <UIcon v-else name="i-lucide-image" class="size-6 text-toned" aria-hidden="true" />
                  </div>
                  <div class="min-w-0">
                    <p class="truncate font-medium text-highlighted" :title="selectedPhoto?.name">
                      {{ selectedPhoto?.name ?? (editorPhotoUrl ? 'Foto saat ini' : 'Belum ada foto') }}
                    </p>
                    <p class="mt-0.5 text-sm text-toned">
                      {{ selectedPhoto && !photoError ? 'Foto baru siap disimpan' : 'JPG, PNG, atau WebP · Maks. 5 MB' }}
                    </p>
                  </div>
                </div>
                <UButton
                  type="button"
                  :label="editorPhotoUrl ? 'Ganti foto' : 'Pilih foto'"
                  icon="i-lucide-upload"
                  color="primary"
                  variant="soft"
                  class="min-h-11 shrink-0"
                  :disabled="saving"
                  @click="photoInput?.click()"
                />
              </div>
              <div v-if="selectedPhoto || photoError" class="mt-1 flex justify-end">
                <UButton
                  type="button"
                  label="Batal pilih foto"
                  color="neutral"
                  variant="link"
                  class="min-h-11"
                  :disabled="saving"
                  @click="clearSelectedPhoto"
                />
              </div>
            </UFormField>
            <div class="flex flex-wrap items-center justify-between gap-2">
              <!-- Hapus hanya muncul saat mengedit wahana yang sudah tersimpan. -->
              <UButton
                v-if="selectedWahanaId !== null"
                type="button"
                label="Hapus wahana"
                icon="i-lucide-trash-2"
                color="error"
                variant="soft"
                :disabled="saving || deleting || savingFeaturedId !== null"
                @click="requestDeleteEditingWahana"
              />
              <div class="ml-auto flex justify-end gap-2">
                <UButton
                  label="Batal"
                  color="neutral"
                  variant="subtle"
                  :disabled="saving"
                  @click="editorOpen = false"
                />
                <UButton
                  type="submit"
                  :label="selectedWahanaId === null ? 'Simpan wahana' : 'Simpan perubahan'"
                  :disabled="!!photoError"
                  :loading="saving"
                />
              </div>
            </div>
          </UForm>
        </template>
      </UModal>

      <UModal
        v-model:open="deleteModalOpen"
        title="Hapus wahana?"
        :description="pendingDeleteWahana ? `Wahana ${pendingDeleteWahana.name} akan dihapus dari katalog.` : 'Tindakan ini tidak bisa dibatalkan.'"
        :dismissible="!deleting"
        :close="!deleting"
      >
        <template #body>
          <div class="space-y-4">
            <p class="text-sm text-muted">
              Tiket yang sudah terbit, riwayat kunjungan, dan foto wahana tetap disimpan.
            </p>
            <UAlert
              v-if="(pendingDeleteWahana?.ticket_owner_count ?? 0) > 0"
              color="warning"
              variant="subtle"
              icon="i-lucide-triangle-alert"
              title="Masih ada pengunjung dengan tiket"
              :description="`Ada ${pendingDeleteWahana?.ticket_owner_count} pengunjung dengan ${pendingDeleteWahana?.ticket_count} tiket terkait. Wahana akan disembunyikan dari katalog; tiket dan riwayatnya tetap disimpan.`"
            />
            <UAlert
              v-if="deleteErrorMessage"
              color="error"
              variant="subtle"
              :description="deleteErrorMessage"
            />
            <div class="flex justify-end gap-2">
              <UButton
                label="Batal"
                color="neutral"
                variant="subtle"
                :disabled="deleting"
                @click="deleteModalOpen = false"
              />
              <UButton
                label="Hapus wahana"
                color="error"
                variant="solid"
                :loading="deleting"
                :disabled="!pendingDeleteWahana"
                @click="deleteWahana"
              />
            </div>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>
