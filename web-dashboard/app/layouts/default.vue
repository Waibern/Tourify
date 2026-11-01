<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const auth = useDashboardAuth()

// Aktivitas baru tetap terpantau di seluruh halaman admin.
useDashboardActivity()

const open = ref(false)

// Tutup menu HP saat sidebar menetap agar overlay tidak menyisakan kunci fokus.
const sidebarVisible = useMediaQuery('(min-width: 500px)')
watch(sidebarVisible, (visible) => {
  if (visible) {
    open.value = false
  }
})

// Menu utama hanya menampilkan halaman yang bisa dibuka role saat ini.
const links = computed<NavigationMenuItem[]>(() => {
  const isAdmin = auth.user.value?.role === 'admin'
  const primary: NavigationMenuItem[] = []

  if (isAdmin) {
    primary.push({
      label: 'Ringkasan',
      icon: 'i-lucide-chart-no-axes-combined',
      to: '/',
      onSelect: () => {
        open.value = false
      }
    }, {
      label: 'Pesanan & Tiket',
      icon: 'i-lucide-receipt-text',
      to: '/orders',
      onSelect: () => { open.value = false }
    }, {
      label: 'Riwayat Check-in',
      icon: 'i-lucide-history',
      to: '/checkins',
      onSelect: () => { open.value = false }
    }, {
      label: 'Pelanggan',
      icon: 'i-lucide-users',
      to: '/customers',
      onSelect: () => {
        open.value = false
      }
    })
  }

  primary.push({
    label: 'Pemindai QR',
    icon: 'i-lucide-qr-code',
    to: '/scanner',
    onSelect: () => {
      open.value = false
    }
  })

  if (isAdmin) {
    primary.push({
      label: 'Wahana',
      icon: 'i-lucide-ticket',
      to: '/wahana',
      onSelect: () => {
        open.value = false
      }
    })
  }

  return primary
})
</script>

<template>
  <UDashboardGroup unit="rem">
    <UDashboardSidebar
      id="default"
      v-model:open="open"
      collapsible
      resizable
      class="bg-elevated/25"
      :ui="{ footer: 'min-[500px]:border-t min-[500px]:border-default' }"
    >
      <template #header="{ collapsed }">
        <TeamsMenu :collapsed="collapsed" />
      </template>

      <template #default="{ collapsed }">
        <!-- Jarak antar tautan dibuat lega supaya navigasi mudah dipindai. -->
        <UNavigationMenu
          :collapsed="collapsed"
          :items="links"
          orientation="vertical"
          :ui="{ list: 'space-y-2' }"
          tooltip
          popover
        />
      </template>

      <template #footer="{ collapsed }">
        <UserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <slot />
  </UDashboardGroup>
</template>
