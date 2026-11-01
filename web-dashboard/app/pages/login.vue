<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

definePageMeta({ layout: 'auth' })

const auth = useDashboardAuth()
const state = reactive({ username: '', password: '' })
const pending = ref(false)
const errorMessage = ref('')

const schema = z.object({
  username: z.string().trim().min(1, 'Username wajib diisi.'),
  password: z.string().min(1, 'Password wajib diisi.')
})

type LoginForm = z.output<typeof schema>

// Login hanya menerima akun yang sudah diberi role admin atau gate di server.
async function onSubmit(event: FormSubmitEvent<LoginForm>) {
  pending.value = true
  errorMessage.value = ''

  try {
    const user = await auth.login(event.data.username, event.data.password)
    await navigateTo(user.role === 'gate' ? '/scanner' : '/wahana')
  } catch (error) {
    errorMessage.value = getTourifyErrorMessage(error, 'Login gagal. Coba lagi.')
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <UCard>
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
</template>
