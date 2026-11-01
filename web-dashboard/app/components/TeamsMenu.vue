<script setup lang="ts">
defineProps<{
  collapsed?: boolean
}>()
</script>

<template>
  <!-- Header memakai nama produk, bukan pilihan tim contoh dari template. -->
  <div class="flex h-10 items-center gap-2 px-2 text-highlighted">
    <img
      src="/logo-putih.svg"
      alt=""
      aria-hidden="true"
      class="h-6 w-auto shrink-0 invert dark:invert-0"
    >
    <span v-if="!collapsed" class="truncate font-semibold">Tourify</span>
  </div>
</template>
