<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import type { DashboardApiResponse } from '~/types/tourify'

const fileRef = ref<HTMLInputElement>()
const auth = useDashboardAuth()
const api = useTourifyApi()
const toast = useToast()
const profileImagePath = ref<string | null>(auth.user.value?.profile_image ?? null)
const previewUrl = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const isLoadingProfile = ref(true)
const isUploading = ref(false)
const profileState = reactive({ username: auth.user.value?.username ?? '' })

const profileImageUrl = computed(() => previewUrl.value ?? api.assetUrl(profileImagePath.value))
const savedUsername = ref(profileState.username)
const hasProfileChanges = computed(() => (
  profileState.username.trim() !== savedUsername.value || selectedFile.value !== null
))

const profileSchema = z.object({
  username: z.string().trim().min(1, 'Username wajib diisi.').max(50, 'Username maksimal 50 karakter.')
})

type ProfileSchema = z.output<typeof profileSchema>

onMounted(async () => {
  try {
    const response = await api.get<DashboardApiResponse>('profile.php')
    profileImagePath.value = response.profile?.profile_image ?? null
    profileState.username = response.profile?.username ?? auth.user.value?.username ?? ''
    savedUsername.value = profileState.username
    if (auth.user.value) {
      auth.user.value = { ...auth.user.value, profile_image: profileImagePath.value }
    }
  } catch (error) {
    toast.add({
      title: 'Profil gagal dimuat',
      description: getTourifyErrorMessage(error, 'Periksa koneksi server lalu coba lagi.'),
      color: 'error'
    })
  } finally {
    isLoadingProfile.value = false
  }
})

function openFilePicker() {
  fileRef.value?.click()
}

// Foto mengikuti batas dan format unggahan yang diterima API Tourify.
function stageProfileImage(file: File) {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    toast.add({
      title: 'Format foto belum didukung',
      description: 'Pilih foto JPG, PNG, atau WebP.',
      color: 'error'
    })
    return
  }
  if (file.size < 1 || file.size > 5 * 1024 * 1024) {
    toast.add({
      title: 'Ukuran foto tidak sesuai',
      description: 'Ukuran foto maksimal 5 MB.',
      color: 'error'
    })
    return
  }

  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) stageProfileImage(file)
}

function clearPreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  selectedFile.value = null
}

async function saveProfile(event: FormSubmitEvent<ProfileSchema>) {
  if (!hasProfileChanges.value || isUploading.value) return

  isUploading.value = true
  try {
    // Username dan foto dikirim bersama saat keduanya berubah.
    const response = selectedFile.value
      ? await api.upload<DashboardApiResponse>('profile.php', selectedFile.value, { username: event.data.username })
      : await api.put<DashboardApiResponse>('profile.php', { username: event.data.username })
    if (!response.success || !response.profile) {
      throw new Error(response.message || 'Profil gagal disimpan.')
    }

    profileState.username = response.profile.username
    savedUsername.value = response.profile.username
    profileImagePath.value = response.profile.profile_image
    if (auth.user.value) {
      auth.user.value = {
        ...auth.user.value,
        username: profileState.username,
        profile_image: profileImagePath.value
      }
    }
    clearPreview()
    toast.add({
      title: 'Profil diperbarui',
      description: 'Username dan foto profil berhasil disimpan.',
      color: 'success'
    })
  } catch (error) {
    toast.add({
      title: 'Profil gagal disimpan',
      description: getTourifyErrorMessage(error, 'Periksa koneksi server lalu coba lagi.'),
      color: 'error'
    })
  } finally {
    isUploading.value = false
  }
}

onBeforeUnmount(clearPreview)
</script>

<template>
  <UForm
    id="settings"
    :schema="profileSchema"
    :state="profileState"
    @submit="saveProfile"
  >
    <UPageCard
      title="Profil"
      description="Kelola username dan foto profil akun dashboard."
      variant="naked"
      orientation="horizontal"
      class="mb-4"
    >
      <UButton
        form="settings"
        label="Simpan perubahan"
        color="neutral"
        type="submit"
        class="w-fit lg:ms-auto"
        :loading="isUploading"
        :disabled="isLoadingProfile || isUploading || !hasProfileChanges"
      />
    </UPageCard>

    <UPageCard variant="subtle">
      <UFormField
        name="username"
        label="Username"
        description="Username untuk masuk ke dashboard Tourify."
        required
        class="flex max-sm:flex-col justify-between items-start gap-4"
      >
        <UInput
          v-model="profileState.username"
          autocomplete="username"
          maxlength="50"
          :disabled="isLoadingProfile || isUploading"
        />
      </UFormField>
      <USeparator />
      <UFormField
        name="avatar"
        label="Foto profil"
        description="JPG, PNG, atau WebP. Maksimal 5 MB."
        class="flex max-sm:flex-col justify-between sm:items-center gap-4"
      >
        <div class="flex flex-wrap items-center gap-3">
          <UAvatar
            :src="profileImageUrl"
            :alt="'Foto profil ' + (profileState.username || 'akun Tourify')"
            :text="profileState.username.slice(0, 1).toUpperCase() || '?'"
            size="lg"
          />
          <UButton
            label="Pilih foto"
            color="neutral"
            type="button"
            :disabled="isLoadingProfile || isUploading"
            @click="openFilePicker"
          />
          <input
            ref="fileRef"
            type="file"
            class="hidden"
            accept=".jpg, .jpeg, .png, .webp"
            @change="onFileChange"
          >
        </div>
      </UFormField>
    </UPageCard>
  </UForm>
</template>
