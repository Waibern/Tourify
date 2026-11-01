<script setup lang="ts">
import type { CSSProperties } from 'vue'
import type { DashboardApiResponse } from '~/types/tourify'

const fileRef = ref<HTMLInputElement>()
const videoRef = ref<HTMLVideoElement>()
const auth = useDashboardAuth()
const api = useTourifyApi()
const toast = useToast()
const profileImagePath = ref<string | null>(auth.user.value?.profile_image ?? null)
const isLoadingProfile = ref(true)
const isUploading = ref(false)
const cropPreviewUrl = ref<string | null>(null)
const selectedImageFile = ref<File | null>(null)
const imageSize = ref({ width: 0, height: 0 })
const zoom = ref(1)
const positionX = ref(50)
const positionY = ref(50)
const isCameraOpen = ref(false)
const cameraReady = ref(false)
const cameraStream = ref<MediaStream | null>(null)
const cropFrameSize = 224

// Path gambar disusun dari base URL API agar cocok di browser dan Android.
const profileImageUrl = computed(() => api.assetUrl(profileImagePath.value))
const username = computed(() => auth.user.value?.username ?? '')
const cropImageStyle = computed<CSSProperties>(() => {
  const { width, height } = imageSize.value
  if (!width || !height) return { visibility: 'hidden' as const }

  const scale = cropFrameSize / Math.min(width, height) * zoom.value
  const renderedWidth = width * scale
  const renderedHeight = height * scale

  return {
    width: `${renderedWidth}px`,
    height: `${renderedHeight}px`,
    left: `${-(renderedWidth - cropFrameSize) * positionX.value / 100}px`,
    top: `${-(renderedHeight - cropFrameSize) * positionY.value / 100}px`
  }
})

onMounted(async () => {
  try {
    const response = await api.get<DashboardApiResponse>('profile.php')
    profileImagePath.value = response.profile?.profile_image ?? null
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

function onFileClick() {
  fileRef.value?.click()
}

// Pilih foto dulu supaya posisi dan zoom bisa diatur sebelum diunggah.
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

  if (cropPreviewUrl.value) URL.revokeObjectURL(cropPreviewUrl.value)
  selectedImageFile.value = file
  cropPreviewUrl.value = URL.createObjectURL(file)
  imageSize.value = { width: 0, height: 0 }
  zoom.value = 1
  positionX.value = 50
  positionY.value = 50
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) stageProfileImage(file)
}

function onCropImageLoad(event: Event) {
  const image = event.target as HTMLImageElement
  imageSize.value = { width: image.naturalWidth, height: image.naturalHeight }
}

function clearCropSelection() {
  if (cropPreviewUrl.value) URL.revokeObjectURL(cropPreviewUrl.value)
  cropPreviewUrl.value = null
  selectedImageFile.value = null
  imageSize.value = { width: 0, height: 0 }
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (blob) resolve(blob)
      else reject(new Error('Foto gagal diproses. Coba pilih foto lain.'))
    }, type, quality)
  })
}

// Hasil crop dibuat sama dengan pratinjau supaya unggahan tetap berbentuk persegi.
async function cropSelectedImage(): Promise<File> {
  if (!cropPreviewUrl.value || !selectedImageFile.value || !imageSize.value.width) {
    throw new Error('Tunggu sampai pratinjau foto selesai dimuat.')
  }

  const image = new Image()
  image.src = cropPreviewUrl.value
  await image.decode()

  const sourceSize = Math.min(image.naturalWidth, image.naturalHeight) / zoom.value
  const sourceX = (image.naturalWidth - sourceSize) * positionX.value / 100
  const sourceY = (image.naturalHeight - sourceSize) * positionY.value / 100
  const canvas = document.createElement('canvas')
  canvas.width = 768
  canvas.height = 768
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Foto gagal diproses. Coba pilih foto lain.')

  context.fillStyle = '#fff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(
    image,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    canvas.width,
    canvas.height
  )
  const blob = await canvasToBlob(canvas, 'image/jpeg', 0.9)
  return new File([blob], 'profile.jpg', { type: 'image/jpeg' })
}

async function saveProfileImage() {
  isUploading.value = true
  try {
    const file = await cropSelectedImage()
    const response = await api.upload<DashboardApiResponse>('profile.php', file)
    if (!response.success || !response.profile) {
      throw new Error(response.message || 'Foto profil gagal disimpan.')
    }
    profileImagePath.value = response.profile.profile_image
    if (auth.user.value) {
      auth.user.value = { ...auth.user.value, profile_image: profileImagePath.value }
    }
    clearCropSelection()
    toast.add({
      title: 'Foto profil diperbarui',
      description: 'Foto ini juga dipakai oleh aplikasi Tourify.',
      color: 'success'
    })
  } catch (error) {
    toast.add({
      title: 'Foto profil gagal disimpan',
      description: getTourifyErrorMessage(error, 'Periksa koneksi server lalu coba lagi.'),
      color: 'error'
    })
  } finally {
    isUploading.value = false
  }
}

// Kamera hanya aktif setelah tombol ditekan dan stream selalu dihentikan saat ditutup.
async function openCamera() {
  if (!navigator.mediaDevices?.getUserMedia) {
    toast.add({
      title: 'Kamera tidak tersedia',
      description: 'Buka dashboard lewat localhost atau HTTPS untuk memakai kamera.',
      color: 'error'
    })
    return
  }

  try {
    cameraStream.value = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'user' },
      audio: false
    })
    cameraReady.value = false
    isCameraOpen.value = true
    await nextTick()
    if (!videoRef.value) return stopCamera()
    videoRef.value.srcObject = cameraStream.value
    await videoRef.value.play()
  } catch (error) {
    stopCamera()
    toast.add({
      title: 'Kamera tidak bisa dibuka',
      description: error instanceof DOMException && error.name === 'NotAllowedError'
        ? 'Izinkan akses kamera di browser lalu coba lagi.'
        : 'Pastikan kamera tersambung dan izinnya aktif.',
      color: 'error'
    })
  }
}

function stopCamera() {
  cameraStream.value?.getTracks().forEach(track => track.stop())
  cameraStream.value = null
  if (videoRef.value) videoRef.value.srcObject = null
  cameraReady.value = false
  isCameraOpen.value = false
}

async function captureCamera() {
  const video = videoRef.value
  if (!video || !cameraReady.value) return

  // Ukuran kamera dibatasi supaya hasil foto tetap ringan untuk diunggah.
  const scale = Math.min(1, 1920 / Math.max(video.videoWidth, video.videoHeight))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(video.videoWidth * scale)
  canvas.height = Math.round(video.videoHeight * scale)
  const context = canvas.getContext('2d')
  if (!context) {
    toast.add({ title: 'Foto gagal diambil', description: 'Coba buka kamera lagi.', color: 'error' })
    return
  }

  context.drawImage(video, 0, 0, canvas.width, canvas.height)
  try {
    const blob = await canvasToBlob(canvas, 'image/jpeg', 0.88)
    stopCamera()
    stageProfileImage(new File([blob], 'profile-camera.jpg', { type: 'image/jpeg' }))
  } catch {
    toast.add({ title: 'Foto gagal diambil', description: 'Coba buka kamera lagi.', color: 'error' })
  }
}

onBeforeUnmount(() => {
  stopCamera()
  clearCropSelection()
})
</script>

<template>
  <!-- Profil memakai path gambar yang sama dengan aplikasi Tourify. -->
  <UPageCard
    title="Profil"
    description="Foto profil akun dashboard Tourify."
    variant="naked"
    orientation="horizontal"
    class="mb-4"
  />

  <UPageCard variant="subtle">
    <UFormField
      name="avatar"
      label="Foto profil"
      description="Atur posisi dan zoom sebelum menyimpan, atau ambil foto lewat kamera. JPG, PNG, atau WebP; maksimal 5 MB."
      class="flex max-sm:flex-col justify-between sm:items-center gap-4"
    >
      <div class="flex w-full flex-col gap-5">
        <div class="flex flex-wrap items-center gap-3">
          <UAvatar
            :src="profileImageUrl"
            :text="username ? username.slice(0, 1).toUpperCase() : '?'"
            :alt="'Foto profil ' + (username || 'akun Tourify')"
            size="xl"
          />
          <div class="flex flex-col gap-1">
            <p class="font-medium text-highlighted">
              {{ username || 'Akun Tourify' }}
            </p>
            <p class="text-sm text-muted">
              {{ isUploading ? 'Menyimpan foto…' : cropPreviewUrl ? 'Atur foto lalu simpan perubahan.' : 'Foto ini tampil di dashboard dan aplikasi Tourify.' }}
            </p>
          </div>
          <UButton
            label="Pilih foto"
            icon="i-lucide-image-plus"
            color="neutral"
            variant="outline"
            :disabled="isLoadingProfile || isUploading"
            @click="onFileClick"
          />
          <UButton
            label="Ambil foto"
            icon="i-lucide-camera"
            color="neutral"
            variant="outline"
            :disabled="isLoadingProfile || isUploading"
            @click="openCamera"
          />
          <input
            ref="fileRef"
            type="file"
            class="hidden"
            accept="image/jpeg, image/png, image/webp"
            @change="onFileChange"
          >
        </div>

        <div
          v-if="isCameraOpen"
          class="flex flex-col gap-3 rounded-xl border border-default bg-elevated/30 p-4"
        >
          <video
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="max-h-96 w-full rounded-lg bg-black object-cover"
            @loadedmetadata="cameraReady = true"
          />
          <div class="flex flex-wrap gap-2">
            <UButton
              label="Ambil foto"
              icon="i-lucide-camera"
              :disabled="!cameraReady"
              @click="captureCamera"
            />
            <UButton
              label="Tutup kamera"
              color="neutral"
              variant="outline"
              @click="stopCamera"
            />
          </div>
        </div>

        <div
          v-if="cropPreviewUrl"
          class="flex flex-col gap-5 border-t border-default pt-5 sm:flex-row"
        >
          <div class="relative mx-auto size-56 shrink-0 overflow-hidden rounded-full bg-elevated">
            <img
              :src="cropPreviewUrl"
              :style="cropImageStyle"
              alt="Pratinjau foto profil"
              draggable="false"
              class="absolute max-w-none select-none"
              @load="onCropImageLoad"
            >
          </div>

          <div class="flex min-w-0 flex-1 flex-col gap-4">
            <p class="text-sm text-muted">
              Atur foto sampai pas di lingkaran pratinjau.
            </p>
            <label for="profile-zoom" class="flex justify-between text-sm font-medium text-highlighted">
              <span>Zoom</span>
              <span>{{ Math.round(zoom * 100) }}%</span>
            </label>
            <input
              id="profile-zoom"
              v-model.number="zoom"
              type="range"
              min="1"
              max="3"
              step="0.05"
              class="w-full accent-primary"
              :disabled="isUploading"
            >
            <label for="profile-position-x" class="text-sm font-medium text-highlighted">
              Posisi horizontal
            </label>
            <input
              id="profile-position-x"
              v-model.number="positionX"
              type="range"
              min="0"
              max="100"
              step="1"
              class="w-full accent-primary"
              :disabled="isUploading"
            >
            <label for="profile-position-y" class="text-sm font-medium text-highlighted">
              Posisi vertikal
            </label>
            <input
              id="profile-position-y"
              v-model.number="positionY"
              type="range"
              min="0"
              max="100"
              step="1"
              class="w-full accent-primary"
              :disabled="isUploading"
            >
            <div class="flex flex-wrap gap-2 pt-1">
              <UButton
                label="Simpan foto"
                icon="i-lucide-check"
                :loading="isUploading"
                :disabled="isUploading || !imageSize.width"
                @click="saveProfileImage"
              />
              <UButton
                label="Batal"
                color="neutral"
                variant="outline"
                :disabled="isUploading"
                @click="clearCropSelection"
              />
            </div>
          </div>
        </div>
      </div>
    </UFormField>
  </UPageCard>
</template>
