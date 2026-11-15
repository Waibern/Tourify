<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'

const colorMode = useColorMode()
const appConfig = useAppConfig()
const { asciiAnimations } = useDashboardPreferences()

const colorModeOptions = [
  { label: 'Sesuai perangkat', value: 'system' },
  { label: 'Terang', value: 'light' },
  { label: 'Gelap', value: 'dark' }
]

// Pilihan warna ini dulu ada di menu akun.
const primaryColors = ['red', 'orange', 'amber', 'yellow', 'lime', 'green', 'emerald', 'teal', 'cyan', 'sky', 'blue', 'indigo', 'violet', 'purple', 'fuchsia', 'pink', 'rose']
const neutralColors = ['slate', 'gray', 'zinc', 'neutral', 'stone', 'taupe', 'mauve', 'mist', 'olive']

const primaryColorItems = computed<DropdownMenuItem[][]>(() => [primaryColors.map(color => ({
  label: color,
  chip: color,
  slot: 'chip',
  checked: appConfig.ui.colors.primary === color,
  type: 'checkbox',
  onSelect(event: Event) {
    event.preventDefault()
    appConfig.ui.colors.primary = color
  }
}))])

const neutralColorItems = computed<DropdownMenuItem[][]>(() => [neutralColors.map(color => ({
  label: color,
  chip: color === 'neutral' ? 'old-neutral' : color,
  slot: 'chip',
  checked: appConfig.ui.colors.neutral === color,
  type: 'checkbox',
  onSelect(event: Event) {
    event.preventDefault()
    appConfig.ui.colors.neutral = color
  }
}))])

const primaryLabel = computed(() => appConfig.ui.colors.primary.charAt(0).toUpperCase() + appConfig.ui.colors.primary.slice(1))
const neutralLabel = computed(() => appConfig.ui.colors.neutral.charAt(0).toUpperCase() + appConfig.ui.colors.neutral.slice(1))

function colorChip(color: string) {
  return color === 'neutral' ? 'old-neutral' : color
}

function colorChipStyle(color: string) {
  const chip = colorChip(color)

  return {
    '--chip-light': `var(--color-${chip}-500)`,
    '--chip-dark': `var(--color-${chip}-400)`
  }
}

function updateColorMode(value: string | number) {
  if (value === 'system' || value === 'light' || value === 'dark') {
    colorMode.preference = value
  }
}

function updateAsciiAnimations(enabled: boolean) {
  // Pengaturan ini mematikan animasi ASCII di seluruh dashboard.
  asciiAnimations.value = enabled
}
</script>

<template>
  <div>
    <UPageCard
      title="Tampilan"
      description="Atur tema, warna, dan animasi dashboard."
      variant="naked"
      class="mb-4"
    />

    <UPageCard variant="subtle" :ui="{ container: 'divide-y divide-default' }">
      <UFormField
        label="Tema"
        description="Pilih tema terang, gelap, atau ikuti perangkat."
        class="flex items-center justify-between gap-4 py-4 max-sm:flex-col max-sm:items-start"
      >
        <USelect
          :model-value="colorMode.preference"
          :items="colorModeOptions"
          aria-label="Tema dashboard"
          class="w-48"
          @update:model-value="updateColorMode"
        />
      </UFormField>

      <UFormField
        label="Primary"
        description="Warna aksen untuk tombol dan elemen utama."
        class="flex items-center justify-between gap-4 py-4 max-sm:flex-col max-sm:items-start"
      >
        <UDropdownMenu :items="primaryColorItems">
          <UButton
            color="neutral"
            variant="outline"
            trailing-icon="i-lucide-chevron-down"
            class="w-48 justify-start"
          >
            <template #leading>
              <span class="size-3 rounded-full bg-(--chip-light) ring ring-bg dark:bg-(--chip-dark)" :style="colorChipStyle(appConfig.ui.colors.primary)" />
            </template>
            {{ primaryLabel }}
          </UButton>

          <template #chip-leading="{ item }">
            <div class="inline-flex size-5 shrink-0 items-center justify-center">
              <span
                class="size-2 rounded-full bg-(--chip-light) ring ring-bg dark:bg-(--chip-dark)"
                :style="colorChipStyle(String((item as any).chip))"
              />
            </div>
          </template>
        </UDropdownMenu>
      </UFormField>

      <UFormField
        label="Neutral"
        description="Warna dasar untuk permukaan dan elemen netral."
        class="flex items-center justify-between gap-4 py-4 max-sm:flex-col max-sm:items-start"
      >
        <UDropdownMenu :items="neutralColorItems">
          <UButton
            color="neutral"
            variant="outline"
            trailing-icon="i-lucide-chevron-down"
            class="w-48 justify-start"
          >
            <template #leading>
              <span class="size-3 rounded-full bg-(--chip-light) ring ring-bg dark:bg-(--chip-dark)" :style="colorChipStyle(appConfig.ui.colors.neutral)" />
            </template>
            {{ neutralLabel }}
          </UButton>

          <template #chip-leading="{ item }">
            <div class="inline-flex size-5 shrink-0 items-center justify-center">
              <span
                class="size-2 rounded-full bg-(--chip-light) ring ring-bg dark:bg-(--chip-dark)"
                :style="colorChipStyle(String((item as any).chip))"
              />
            </div>
          </template>
        </UDropdownMenu>
      </UFormField>

      <UFormField
        label="Animasi ASCII"
        description="Matikan untuk menghentikan reveal dan shimmer ASCII di dashboard."
        class="flex items-center justify-between gap-4 py-4"
      >
        <USwitch
          :model-value="asciiAnimations"
          aria-label="Aktifkan animasi ASCII"
          @update:model-value="updateAsciiAnimations"
        />
      </UFormField>
    </UPageCard>
  </div>
</template>
