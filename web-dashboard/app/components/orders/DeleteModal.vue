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
    title="Hapus tiket terpilih?"
    :description="`${props.count} pesanan tiket akan dihapus. Tiket yang sudah terpakai atau memiliki riwayat check-in tidak bisa dihapus.`"
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
            :disabled="loading"
            @click="open = false"
          />
          <UButton
            label="Hapus tiket"
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
