<script setup lang="ts">
defineProps<{
  collapsed?: boolean
}>()
</script>

<template>
  <!-- Header memakai nama produk, bukan pilihan tim contoh dari template. -->
  <div class="flex h-10 items-center gap-2 px-2 text-highlighted">
    <UIcon name="i-lucide-ticket" class="size-5 shrink-0" />
    <span v-if="!collapsed" class="truncate font-semibold">Tourify</span>
  </div>
</template>
