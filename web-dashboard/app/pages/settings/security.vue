<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { DashboardApiResponse } from '~/types/tourify'

const api = useTourifyApi()
const auth = useDashboardAuth()
const toast = useToast()
const saving = ref(false)
const passwordError = ref('')
const deleteOpen = ref(false)
const deletePassword = ref('')
const confirmDelete = ref(false)
const deleting = ref(false)
const deleteError = ref('')

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Masukkan password saat ini.'),
  newPassword: z.string().min(8, 'Password baru minimal 8 karakter.').refine(
    value => new TextEncoder().encode(value).length <= 72,
    'Password maksimal 72 byte.'
  ),
  confirmPassword: z.string().min(8, 'Konfirmasi password minimal 8 karakter.')
}).refine(value => value.newPassword !== value.currentPassword, {
  message: 'Password baru harus berbeda dari password saat ini.',
  path: ['newPassword']
}).refine(value => value.newPassword === value.confirmPassword, {
  message: 'Konfirmasi password belum sama.',
  path: ['confirmPassword']
})

type PasswordSchema = z.output<typeof passwordSchema>

const passwordState = reactive<Partial<PasswordSchema>>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function changePassword(event: FormSubmitEvent<PasswordSchema>) {
  // Server mengecek password saat ini sebelum menyimpan yang baru.
  saving.value = true
  passwordError.value = ''
  try {
    const response = await api.post<DashboardApiResponse>('dashboard_security.php', {
      current_password: event.data.currentPassword,
      new_password: event.data.newPassword
    })
    if (!response.success) throw new Error(response.message || 'Password gagal diperbarui.')

    Object.assign(passwordState, { currentPassword: '', newPassword: '', confirmPassword: '' })
    toast.add({
      title: 'Password diperbarui',
      description: 'Password baru dipakai untuk login berikutnya.',
      color: 'success'
    })
  } catch (error) {
    passwordError.value = getTourifyErrorMessage(error, 'Password gagal diperbarui. Periksa password saat ini lalu coba lagi.')
  } finally {
    saving.value = false
  }
}

async function deleteAccount() {
  if (!deletePassword.value || !confirmDelete.value || deleting.value) return

  // Hapus akun hanya setelah password dan konfirmasi dikirim ke server.
  deleting.value = true
  deleteError.value = ''
  try {
    await api.delete<DashboardApiResponse>('dashboard_security.php', {
      current_password: deletePassword.value,
      confirm: true
    })
    auth.user.value = null
    deleteOpen.value = false
    await navigateTo('/login')
  } catch (error) {
    deleteError.value = getTourifyErrorMessage(error, 'Akun tidak bisa dihapus. Periksa password dan riwayat akun.')
  } finally {
    deleting.value = false
  }
}

watch(deleteOpen, (isOpen) => {
  if (!isOpen) {
    deletePassword.value = ''
    confirmDelete.value = false
    deleteError.value = ''
  }
})
</script>

<template>
  <UPageCard
    title="Password"
    description="Masukkan password saat ini sebelum membuat password baru."
    variant="subtle"
  >
    <UAlert
      v-if="passwordError"
      color="error"
      variant="subtle"
      :description="passwordError"
      class="mb-4"
    />
    <UForm
      :schema="passwordSchema"
      :state="passwordState"
      class="flex flex-col gap-4 max-w-xs"
      @submit="changePassword"
    >
      <UFormField label="Password saat ini" name="currentPassword">
        <UInput
          v-model="passwordState.currentPassword"
          type="password"
          autocomplete="current-password"
          class="w-full"
        />
      </UFormField>
      <UFormField label="Password baru" name="newPassword">
        <UInput
          v-model="passwordState.newPassword"
          type="password"
          autocomplete="new-password"
          class="w-full"
        />
      </UFormField>
      <UFormField label="Ulangi password baru" name="confirmPassword">
        <UInput
          v-model="passwordState.confirmPassword"
          type="password"
          autocomplete="new-password"
          class="w-full"
        />
      </UFormField>
      <UButton
        label="Simpan perubahan"
        class="w-fit"
        type="submit"
        :loading="saving"
      />
    </UForm>
  </UPageCard>

  <UPageCard
    title="Akun"
    description="Hapus akun dashboard ini secara permanen. Akun dengan riwayat pesanan atau check-in tidak dapat dihapus."
    class="bg-linear-to-tl from-error/10 from-5% to-default"
  >
    <template #footer>
      <UModal
        v-model:open="deleteOpen"
        title="Hapus akun"
        description="Tindakan ini tidak bisa dibatalkan."
      >
        <UButton label="Hapus akun" color="error" />

        <template #body>
          <UAlert
            v-if="deleteError"
            color="error"
            variant="subtle"
            :description="deleteError"
            class="mb-4"
          />
          <div class="flex flex-col gap-4">
            <UFormField label="Password saat ini" name="delete-password">
              <UInput
                v-model="deletePassword"
                type="password"
                autocomplete="current-password"
                class="w-full"
              />
            </UFormField>
            <UCheckbox
              v-model="confirmDelete"
              label="Saya paham akun ini akan dihapus permanen."
            />
            <div class="flex justify-end gap-2">
              <UButton
                label="Batal"
                color="neutral"
                variant="subtle"
                @click="deleteOpen = false"
              />
              <UButton
                label="Hapus akun"
                color="error"
                :disabled="!deletePassword || !confirmDelete"
                :loading="deleting"
                @click="deleteAccount"
              />
            </div>
          </div>
        </template>
      </UModal>
    </template>
  </UPageCard>
</template>
