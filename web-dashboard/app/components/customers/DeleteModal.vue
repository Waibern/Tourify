<script setup lang="ts">
const props = defineProps<{
  count: number
  loading?: boolean
  error?: string
}>()

const open = defineModel<boolean>('open', { default: false })
const emit = defineEmits<{ confirm: [] }>()
</script>

<template>
  <UModal
    v-model:open="open"
    title="Hapus pelanggan terpilih?"
    :description="`${props.count} akun pelanggan akan dihapus. Akun dengan riwayat pesanan tidak bisa dihapus.`"
  >
    <slot />

    <template #body>
      <div class="space-y-4">
        <UAlert
          v-if="error"
          color="error"
          variant="subtle"
          :description="error"
        />
        <div class="flex justify-end gap-2">
          <UButton
            label="Batal"
            color="neutral"
            variant="subtle"
            @click="open = false"
          />
          <UButton
            label="Hapus akun"
            color="error"
            variant="solid"
            :loading="loading"
            :disabled="loading || count === 0"
            @click="emit('confirm')"
          />
        </div>
      </div>
    </template>
  </UModal>
</template>
