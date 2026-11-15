<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'

definePageMeta({ layout: 'auth' })

const auth = useDashboardAuth()
const state = reactive({ username: '', password: '' })
const pending = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const loginRevealRoot = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()
const lastUsernameKey = 'tourify-last-login-username'
let revealController: AsciiRevealController | undefined
let revealPromise: Promise<void> | undefined

// Cek sesi setelah halaman login selesai dimuat.
async function redirectSavedSession() {
  const user = await auth.loadSession()
  if (user) {
    await navigateTo(user.role === 'gate' ? '/scanner' : '/')
  }
}

// Matikan animasi pembuka kalau pengguna memilih gerakan yang lebih ringan.
onMounted(() => {
  // Isi username terakhir dari browser; password tidak disimpan.
  state.username = localStorage.getItem(lastUsernameKey) ?? ''
  void redirectSavedSession()

  const root = loginRevealRoot.value
  const surface = root?.querySelector<HTMLElement>('[data-ascii="surface"]')
  if (!root || !surface || reducedMotion.value === 'reduce' || !asciiAnimations.value) return

  revealController = createAsciiReveal(root, {
    preset: 'decode',
    duration: 'cinematic',
    lazy: false
  })
  const controller = revealController
  revealPromise = controller.reveal({ within: surface }).catch(() => {
    controller.destroy()
    if (revealController === controller) {
      revealController = undefined
    }
  })
})

watch(asciiAnimations, (enabled) => {
  if (enabled) return

  revealController?.destroy()
  revealController = undefined
  revealPromise = undefined
})

// Hentikan animasi saat meninggalkan halaman login.
onBeforeUnmount(() => {
  revealController?.destroy()
  revealController = undefined
})

const schema = z.object({
  username: z.string().trim().min(1, 'Username wajib diisi.'),
  password: z.string().min(1, 'Password wajib diisi.')
})

type LoginForm = z.output<typeof schema>

// Server hanya menerima akun admin atau petugas yang sudah terdaftar.
async function onSubmit(event: FormSubmitEvent<LoginForm>) {
  if (pending.value) return
  pending.value = true
  errorMessage.value = ''
  let formHidden = false

  try {
    const user = await auth.login(event.data.username, event.data.password)
    // Simpan username setelah login berhasil supaya tidak perlu mengetik lagi.
    localStorage.setItem(lastUsernameKey, event.data.username.trim())

    if (revealController && reducedMotion.value !== 'reduce' && asciiAnimations.value) {
      try {
        await revealPromise
        const surface = loginRevealRoot.value?.querySelector<HTMLElement>('[data-ascii="surface"]')
        if (surface) {
          await revealController.hide({
            within: surface,
            duration: 'fast'
          })
          formHidden = true
        }
      } catch {
        // Login tetap berhasil meski animasinya gagal.
      }
    }
    await navigateTo(user.role === 'gate' ? '/scanner' : '/')
  } catch (error) {
    if (formHidden && revealController) {
      const surface = loginRevealRoot.value?.querySelector<HTMLElement>('[data-ascii="surface"]')
      if (surface) {
        await revealController.reveal({ within: surface })
      }
    }
    errorMessage.value = getTourifyErrorMessage(error, 'Login gagal. Coba lagi.')
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div ref="loginRevealRoot" class="mx-auto w-full max-w-[24rem] min-w-0">
    <!-- Animasi pembuka menampilkan form tanpa kotak tambahan. -->
    <section
      data-ascii="surface"
      class="space-y-5"
    >
      <div class="space-y-2">
        <h1 class="text-2xl font-semibold leading-tight tracking-[-0.03em] text-white sm:text-[1.75rem]">
          Selamat datang kembali
        </h1>
        <p class="text-xs leading-5 text-white/70 sm:text-sm">
          Gunakan akun admin atau petugas gate.
        </p>
      </div>

      <UAlert
        v-if="errorMessage"
        color="error"
        variant="subtle"
        :description="errorMessage"
      />

      <!-- Garis putih menunjukkan kolom yang sedang dipilih dengan keyboard. -->
      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <!-- Ikon membantu mengenali kolom username. -->
        <UFormField
          label="Username"
          name="username"
          :ui="{ label: 'text-xs font-medium text-white/75', labelWrapper: 'mb-2' }"
        >
          <div class="relative">
            <UIcon
              name="i-lucide-user-round"
              class="pointer-events-none absolute left-4 top-1/2 z-10 size-5 -translate-y-1/2 text-white/45"
              aria-hidden="true"
            />
            <UInput
              v-model="state.username"
              autocomplete="username"
              placeholder="Masukkan username"
              size="lg"
              class="w-full"
              :ui="{ base: 'h-12 rounded-2xl border border-transparent bg-[#242426] pl-12 pr-4 text-base text-white placeholder:text-white/50 focus-visible:border-white focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#0e0e0e]' }"
            />
          </div>
        </UFormField>

        <!-- Tombol mata bisa menampilkan atau menyembunyikan password. -->
        <UFormField
          label="Password"
          name="password"
          :ui="{ label: 'text-xs font-medium text-white/75', labelWrapper: 'mb-2' }"
        >
          <div class="relative">
            <UIcon
              name="i-lucide-lock-keyhole"
              class="pointer-events-none absolute left-4 top-1/2 z-10 size-5 -translate-y-1/2 text-white/45"
              aria-hidden="true"
            />
            <UInput
              v-model="state.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="Masukkan password"
              size="lg"
              class="w-full"
              :ui="{ base: 'h-12 rounded-2xl border border-transparent bg-[#242426] pl-12 pr-14 text-base text-white placeholder:text-white/50 focus-visible:border-white focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#0e0e0e]' }"
            />
            <button
              type="button"
              class="absolute right-1.5 top-1/2 z-10 inline-flex size-11 -translate-y-1/2 items-center justify-center rounded-lg text-white/55 transition-colors hover:bg-white/5 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-400"
              :aria-label="showPassword ? 'Sembunyikan password' : 'Tampilkan password'"
              :aria-pressed="showPassword"
              @click="showPassword = !showPassword"
            >
              <UIcon
                :name="showPassword ? 'i-lucide-eye-off' : 'i-lucide-eye'"
                class="size-5"
                aria-hidden="true"
              />
            </button>
          </div>
        </UFormField>

        <!-- Tombol masuk menampilkan label dan panah di tengah. -->
        <UButton
          type="submit"
          color="neutral"
          variant="solid"
          size="lg"
          block
          :loading="pending"
          class="h-12 rounded-full bg-white text-base font-semibold text-neutral-950 transition-transform hover:bg-neutral-100 active:scale-[0.99]"
        >
          <span class="flex items-center justify-center gap-2.5">
            <span>Masuk</span>
            <UIcon
              name="i-lucide-arrow-right"
              class="size-[18px]"
              aria-hidden="true"
            />
          </span>
        </UButton>
      </UForm>
    </section>
  </div>
</template>
