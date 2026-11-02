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
const lastUsernameKey = 'tourify-last-login-username'
let revealController: AsciiRevealController | undefined
let revealPromise: Promise<void> | undefined

// Form dibuka dengan efek ASCII, kecuali pengguna memilih kurangi animasi.
onMounted(() => {
  // Isi username terakhir dari browser tanpa menyimpan password.
  state.username = localStorage.getItem(lastUsernameKey) ?? ''

  const root = loginRevealRoot.value
  const surface = root?.querySelector<HTMLElement>('[data-ascii="surface"]')
  if (!root || !surface || reducedMotion.value === 'reduce') return

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

// Bersihkan kanvas animasi saat form login ditinggalkan.
onBeforeUnmount(() => {
  revealController?.destroy()
  revealController = undefined
})

const schema = z.object({
  username: z.string().trim().min(1, 'Username wajib diisi.'),
  password: z.string().min(1, 'Password wajib diisi.')
})

type LoginForm = z.output<typeof schema>

// Login hanya menerima akun yang sudah diberi role admin atau gate di server.
async function onSubmit(event: FormSubmitEvent<LoginForm>) {
  if (pending.value) return
  pending.value = true
  errorMessage.value = ''
  let formHidden = false

  try {
    const user = await auth.login(event.data.username, event.data.password)
    // Simpan username setelah login berhasil supaya siap saat kembali.
    localStorage.setItem(lastUsernameKey, event.data.username.trim())

    if (revealController && reducedMotion.value !== 'reduce') {
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
        // Animasi yang gagal tidak boleh menghalangi login berhasil.
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
    <!-- Form tetap jadi target reveal ASCII tanpa kotak pembungkus. -->
    <section
      data-ascii="surface"
      class="space-y-5"
    >
      <div class="space-y-2">
        <h1 class="text-2xl font-semibold leading-tight tracking-[-0.03em] text-white sm:text-[1.75rem]">
          Selamat datang kembali
        </h1>
        <p class="text-sm leading-6 text-white/70 sm:text-base">
          Gunakan akun admin atau petugas gate.
        </p>
      </div>

      <UAlert
        v-if="errorMessage"
        color="error"
        variant="subtle"
        :description="errorMessage"
      />

      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <!-- Kolom username memakai ikon sebagai petunjuk visual. -->
        <UFormField
          label="Username"
          name="username"
          :ui="{ label: 'text-sm font-medium text-white/75', labelWrapper: 'mb-2' }"
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
              :ui="{ base: 'h-12 rounded-2xl border border-transparent bg-[#242426] pl-12 pr-4 text-base text-white placeholder:text-white/50 focus-visible:border-white/15 focus-visible:ring-2 focus-visible:ring-white/10' }"
            />
          </div>
        </UFormField>

        <!-- Password punya kontrol tampil-sembunyi yang tetap mudah disentuh. -->
        <UFormField
          label="Password"
          name="password"
          :ui="{ label: 'text-sm font-medium text-white/75', labelWrapper: 'mb-2' }"
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
              :ui="{ base: 'h-12 rounded-2xl border border-transparent bg-[#242426] pl-12 pr-14 text-base text-white placeholder:text-white/50 focus-visible:border-white/15 focus-visible:ring-2 focus-visible:ring-white/10' }"
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

        <!-- Tombol putih tetap menonjol dengan label dan panah di tengah. -->
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
