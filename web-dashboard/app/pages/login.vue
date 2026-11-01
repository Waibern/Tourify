<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'
import { createAsciiReveal } from 'ascii-reveal/core'
import type { AsciiRevealController } from 'ascii-reveal/core'

definePageMeta({ layout: 'auth' })

const auth = useDashboardAuth()
const state = reactive({ username: '', password: '' })
const pending = ref(false)
const errorMessage = ref('')
const loginRevealRoot = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()
let revealController: AsciiRevealController | undefined
let revealPromise: Promise<void> | undefined

// Form dibuka dengan efek ASCII, kecuali pengguna memilih kurangi animasi.
onMounted(() => {
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
  <div ref="loginRevealRoot" class="w-full min-w-0">
    <UCard data-ascii="surface" data-dashboard-card class="min-w-0">
      <template #header>
        <div>
          <p class="text-sm text-primary font-medium mb-1">
            Tourify
          </p>
          <h1 class="text-xl font-semibold text-highlighted">
            Masuk dashboard
          </h1>
          <p class="text-sm text-muted mt-1">
            Gunakan akun admin atau petugas gate.
          </p>
        </div>
      </template>

      <UAlert
        v-if="errorMessage"
        color="error"
        variant="subtle"
        :description="errorMessage"
        class="mb-4"
      />

      <UForm
        :schema="schema"
        :state="state"
        class="space-y-4"
        @submit="onSubmit"
      >
        <UFormField label="Username" name="username">
          <UInput
            v-model="state.username"
            autocomplete="username"
            class="w-full"
          />
        </UFormField>

        <UFormField label="Password" name="password">
          <UInput
            v-model="state.password"
            type="password"
            autocomplete="current-password"
            class="w-full"
          />
        </UFormField>

        <UButton
          type="submit"
          label="Masuk"
          block
          :loading="pending"
        />
      </UForm>
    </UCard>
  </div>
</template>
