<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const auth = useDashboardAuth()

// Tab pengaturan sama dengan menu di sidebar.
const settingsTabs = computed<NavigationMenuItem[][]>(() => [[
  {
    label: 'Umum',
    icon: 'i-lucide-user-round',
    to: '/settings',
    exact: true
  },
  ...(auth.user.value?.role === 'admin'
    ? [{ label: 'Anggota', icon: 'i-lucide-users-round', to: '/settings/members' }]
    : []),
  {
    label: 'Tampilan',
    icon: 'i-lucide-palette',
    to: '/settings/appearance'
  },
  {
    label: 'Keamanan',
    icon: 'i-lucide-shield-check',
    to: '/settings/security'
  }
]])
</script>

<template>
  <UDashboardPanel id="settings" :ui="{ body: 'lg:py-12' }">
    <template #header>
      <UDashboardNavbar title="Pengaturan">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>

      <UDashboardToolbar>
        <UNavigationMenu
          :items="settingsTabs"
          highlight
          class="-mx-1 flex-1"
        />
      </UDashboardToolbar>
    </template>

    <template #body>
      <div class="flex w-full flex-col gap-4 sm:gap-6 lg:gap-12 lg:max-w-2xl mx-auto">
        <NuxtPage />
      </div>
    </template>
  </UDashboardPanel>
</template>
