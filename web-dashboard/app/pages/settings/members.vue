<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { DashboardMember, DashboardMembersApiResponse, DashboardRole } from '~/types/tourify'

const api = useTourifyApi()
const toast = useToast()
const members = ref<DashboardMember[]>([])
const search = ref('')
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const createError = ref('')
const deleteError = ref('')
const createOpen = ref(false)
const deleteOpen = ref(false)
const creating = ref(false)
const deleting = ref(false)
const updatingMemberId = ref<number | null>(null)
const pendingMember = ref<DashboardMember | null>(null)

const memberSchema = z.object({
  username: z.string().trim().min(1, 'Masukkan username.').max(50, 'Username maksimal 50 karakter.'),
  password: z.string().min(8, 'Password minimal 8 karakter.').refine(
    value => new TextEncoder().encode(value).length <= 72,
    'Password maksimal 72 byte.'
  ),
  role: z.enum(['admin', 'gate'])
})

type MemberSchema = z.output<typeof memberSchema>

const formState = reactive<Partial<MemberSchema>>({
  username: '',
  password: '',
  role: 'gate'
})

const filteredMembers = computed(() => {
  const query = search.value.trim().toLocaleLowerCase('id-ID')
  if (!query) return members.value
  return members.value.filter(member => member.username.toLocaleLowerCase('id-ID').includes(query))
})

async function loadMembers() {
  // API hanya mengembalikan akun admin dan petugas dashboard.
  loading.value = true
  loadError.value = ''
  try {
    const response = await api.get<DashboardMembersApiResponse>('dashboard_members.php')
    members.value = response.members ?? []
  } catch (error) {
    loadError.value = getTourifyErrorMessage(error, 'Daftar anggota gagal dimuat.')
  } finally {
    loading.value = false
  }
}

async function createMember(event: FormSubmitEvent<MemberSchema>) {
  // Password awal dan peran ikut divalidasi kembali oleh endpoint.
  creating.value = true
  createError.value = ''
  try {
    const response = await api.post<DashboardMembersApiResponse>('dashboard_members.php', {
      username: event.data.username,
      password: event.data.password,
      role: event.data.role
    })
    if (!response.member) throw new Error(response.message || 'Akun gagal dibuat.')

    members.value = [...members.value, response.member].sort((left, right) => {
      if (left.role !== right.role) return left.role === 'admin' ? -1 : 1
      return left.username.localeCompare(right.username, 'id-ID')
    })
    createOpen.value = false
    Object.assign(formState, { username: '', password: '', role: 'gate' })
    toast.add({
      title: 'Anggota ditambahkan',
      description: `Akun ${response.member.username} siap digunakan.`,
      color: 'success'
    })
  } catch (error) {
    createError.value = getTourifyErrorMessage(error, 'Akun gagal dibuat. Periksa username lalu coba lagi.')
  } finally {
    creating.value = false
  }
}

async function updateMemberRole(member: DashboardMember, role: DashboardRole) {
  // Server menyimpan peran; sesi lain membaca ulang izinnya pada request berikutnya.
  updatingMemberId.value = member.id
  actionError.value = ''
  try {
    const response = await api.put<DashboardMembersApiResponse>('dashboard_members.php', {
      id: member.id,
      role
    })
    if (!response.member) throw new Error(response.message || 'Peran anggota gagal diperbarui.')
    members.value = members.value.map(item => item.id === member.id ? response.member! : item)
    toast.add({
      title: 'Peran diperbarui',
      description: `${member.username} sekarang ${role === 'admin' ? 'admin' : 'petugas'}.`,
      color: 'success'
    })
  } catch (error) {
    actionError.value = getTourifyErrorMessage(error, 'Peran gagal diperbarui. Muat ulang daftar lalu coba lagi.')
    await loadMembers()
  } finally {
    updatingMemberId.value = null
  }
}

function confirmMemberRemoval(member: DashboardMember) {
  // Riwayat akun diperiksa server sebelum penghapusan dijalankan.
  pendingMember.value = member
  deleteError.value = ''
  deleteOpen.value = true
}

async function removeMember() {
  if (!pendingMember.value || deleting.value) return

  deleting.value = true
  deleteError.value = ''
  try {
    await api.delete<DashboardApiResponse>('dashboard_members.php', { id: pendingMember.value.id })
    members.value = members.value.filter(member => member.id !== pendingMember.value?.id)
    toast.add({
      title: 'Anggota dihapus',
      description: `Akun ${pendingMember.value.username} telah dihapus.`,
      color: 'success'
    })
    deleteOpen.value = false
    pendingMember.value = null
  } catch (error) {
    deleteError.value = getTourifyErrorMessage(error, 'Akun tidak bisa dihapus. Periksa riwayat akun lalu coba lagi.')
  } finally {
    deleting.value = false
  }
}

onMounted(loadMembers)
</script>

<template>
  <div>
    <UPageCard
      title="Anggota"
      description="Kelola akun admin dan petugas dashboard Tourify."
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UModal
        v-model:open="createOpen"
        title="Tambah anggota"
        description="Buat akun dashboard dengan username dan password awal."
      >
        <UButton
          label="Tambah anggota"
          color="neutral"
          class="w-fit lg:ms-auto"
        />

        <template #body>
          <UAlert
            v-if="createError"
            color="error"
            variant="subtle"
            :description="createError"
            class="mb-4"
          />
          <UForm
            :schema="memberSchema"
            :state="formState"
            class="flex flex-col gap-4"
            @submit="createMember"
          >
            <UFormField label="Username" name="username">
              <UInput
                v-model="formState.username"
                autocomplete="username"
                maxlength="50"
                class="w-full"
              />
            </UFormField>
            <UFormField label="Password awal" name="password">
              <UInput
                v-model="formState.password"
                type="password"
                autocomplete="new-password"
                class="w-full"
              />
            </UFormField>
            <UFormField label="Peran" name="role">
              <USelect
                v-model="formState.role"
                :items="[
                  { label: 'Admin', value: 'admin' },
                  { label: 'Petugas', value: 'gate' }
                ]"
                color="neutral"
                class="w-full"
              />
            </UFormField>
            <div class="flex justify-end gap-2">
              <UButton
                label="Batal"
                color="neutral"
                variant="subtle"
                @click="createOpen = false"
              />
              <UButton type="submit" label="Buat akun" :loading="creating" />
            </div>
          </UForm>
        </template>
      </UModal>
    </UPageCard>

    <UPageCard
      variant="subtle"
      :ui="{ container: 'p-0 sm:p-0 gap-y-0', wrapper: 'items-stretch', header: 'p-4 mb-0 border-b border-default' }"
    >
      <template #header>
        <UInput
          v-model="search"
          icon="i-lucide-search"
          placeholder="Cari anggota"
          aria-label="Cari anggota berdasarkan username"
          autofocus
          class="w-full"
        />
      </template>

      <UAlert
        v-if="loadError"
        color="error"
        variant="subtle"
        :description="loadError"
        class="m-4"
      >
        <template #actions>
          <UButton
            label="Coba lagi"
            color="neutral"
            variant="outline"
            @click="loadMembers"
          />
        </template>
      </UAlert>
      <UAlert
        v-else-if="actionError"
        color="error"
        variant="subtle"
        :description="actionError"
        class="m-4"
      >
        <template #actions>
          <UButton
            label="Tutup"
            color="neutral"
            variant="outline"
            @click="actionError = ''"
          />
        </template>
      </UAlert>
      <div v-else-if="loading" class="px-4 py-6 text-sm text-muted" role="status">
        Memuat anggota…
      </div>
      <div v-else-if="!members.length" class="px-4 py-6 text-sm text-muted" role="status">
        Belum ada akun dashboard.
      </div>
      <div v-else-if="!filteredMembers.length" class="px-4 py-6 text-sm text-muted" role="status">
        Tidak ada anggota dengan username tersebut.
      </div>
      <SettingsMembersList
        v-else
        :members="filteredMembers"
        :updating-member-id="updatingMemberId"
        @change-role="updateMemberRole"
        @remove-member="confirmMemberRemoval"
      />
    </UPageCard>

    <UModal
      v-model:open="deleteOpen"
      title="Hapus anggota"
      description="Akun yang memiliki riwayat pesanan atau check-in tidak dapat dihapus."
    >
      <template #body>
        <UAlert
          v-if="deleteError"
          color="error"
          variant="subtle"
          :description="deleteError"
          class="mb-4"
        />
        <p class="mb-4 text-sm text-muted">
          Hapus akun {{ pendingMember?.username }} dari dashboard Tourify?
        </p>
        <div class="flex justify-end gap-2">
          <UButton
            label="Batal"
            color="neutral"
            variant="subtle"
            @click="deleteOpen = false"
          />
          <UButton
            label="Hapus anggota"
            color="error"
            :loading="deleting"
            @click="removeMember"
          />
        </div>
      </template>
    </UModal>
  </div>
</template>
