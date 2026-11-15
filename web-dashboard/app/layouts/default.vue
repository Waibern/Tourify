<script setup lang="ts">
import type { NavigationMenuItem } from '@nuxt/ui'

const auth = useDashboardAuth()
const route = useRoute()

// Notifikasi baru tetap masuk saat admin pindah halaman.
const { newOrderCount, newCustomerCount } = useDashboardActivity()

const open = ref(false)

// Tutup menu samping setelah halaman dipilih.
function closeSidebar() {
  open.value = false
}

// Tutup menu HP saat sidebar terbuka di layar lebar.
const sidebarVisible = useMediaQuery('(min-width: 768px)')
watch(sidebarVisible, (visible) => {
  if (visible) {
    open.value = false
  }
})

// Setiap halaman pengaturan punya tautan sendiri di menu.
const settingsLinks = computed<NavigationMenuItem[]>(() => {
  const items: NavigationMenuItem[] = [{
    label: 'Umum',
    icon: 'i-lucide-user-round',
    to: '/settings',
    exact: true,
    onSelect: closeSidebar
  }]

  if (auth.user.value?.role === 'admin') {
    items.push({
      label: 'Anggota',
      icon: 'i-lucide-users-round',
      to: '/settings/members',
      onSelect: closeSidebar
    })
  }

  items.push({
    label: 'Tampilan',
    icon: 'i-lucide-palette',
    to: '/settings/appearance',
    onSelect: closeSidebar
  }, {
    label: 'Keamanan',
    icon: 'i-lucide-shield-check',
    to: '/settings/security',
    onSelect: closeSidebar
  })

  return items
})

  // Tampilkan halaman yang boleh dibuka oleh peran akun ini.
const links = computed<NavigationMenuItem[]>(() => {
  const isAdmin = auth.user.value?.role === 'admin'
  const primary: NavigationMenuItem[] = []

  if (isAdmin) {
    primary.push({
      label: 'Overview',
      icon: 'i-lucide-chart-no-axes-combined',
      to: '/',
      // Menu aktif dan menu yang disorot memakai bentuk kapsul yang sama.
      class: route.path === '/'
        ? 'rounded-full before:rounded-full bg-[linear-gradient(90deg,rgba(255,255,255,0.14)_0%,rgba(255,255,255,0.025)_24%,rgba(255,255,255,0.025)_76%,rgba(255,255,255,0.14)_100%)] ring-1 ring-inset ring-white/10'
        : undefined,
      onSelect: closeSidebar
    }, {
      label: 'Pesanan & Tiket',
      icon: 'i-lucide-receipt-text',
      to: '/orders',
      badge: newOrderCount.value
        ? {
            label: newOrderCount.value > 99 ? '99+' : newOrderCount.value,
            color: 'neutral',
            variant: 'outline',
            size: 'md',
            class: 'min-w-8 justify-center rounded-md px-2 tabular-nums'
          }
        : undefined,
      onSelect: closeSidebar
    }, {
      label: 'Riwayat Check-in',
      icon: 'i-lucide-history',
      to: '/checkins',
      onSelect: closeSidebar
    }, {
      label: 'Pelanggan',
      icon: 'i-lucide-users',
      to: '/customers',
      badge: newCustomerCount.value
        ? {
            label: newCustomerCount.value > 99 ? '99+' : newCustomerCount.value,
            color: 'neutral',
            variant: 'outline',
            size: 'md',
            class: 'min-w-8 justify-center rounded-md px-2 tabular-nums'
          }
        : undefined,
      onSelect: closeSidebar
    })
  }

  primary.push({
    label: 'Pemindai QR',
    icon: 'i-lucide-qr-code',
    to: '/scanner',
    onSelect: closeSidebar
  })

  if (isAdmin) {
    primary.push({
      label: 'Wahana',
      icon: 'i-lucide-ticket',
      to: '/wahana',
      onSelect: closeSidebar
    })
  }

  // Ikon roda gigi membuka daftar halaman pengaturan.
  primary.push({
    label: 'Pengaturan',
    icon: 'i-lucide-settings',
    to: '/settings',
    type: 'trigger',
    defaultOpen: true,
    children: settingsLinks.value
  })

  // Label menu tetap tersedia saat sidebar hanya menampilkan ikon.
  return primary.map(item => ({
    ...item,
    ...{ 'aria-label': item.label },
    children: item.children?.map(child => ({
      ...child,
      ...{ 'aria-label': child.label }
    }))
  }))
})

// Pencarian hanya menampilkan menu yang boleh dibuka akun ini.
const searchGroups = computed(() => [{
  id: 'navigation',
  label: 'Menu dashboard',
  items: [...links.value, ...settingsLinks.value].map(item => ({
    label: item.label,
    icon: item.icon,
    to: item.to,
    onSelect: item.onSelect
  }))
}])
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
        <UDashboardSearchButton
          label="Cari menu"
          :collapsed="collapsed"
          class="bg-transparent ring-default"
        />

        <!-- Jarak menu dibuat lega supaya tiap pilihan mudah dibaca. -->
        <UNavigationMenu
          :collapsed="collapsed"
          :items="links"
          orientation="vertical"
          :ui="{ list: 'space-y-1', linkLabel: 'whitespace-normal break-words' }"
          tooltip
          popover
        />

      </template>

      <template #footer="{ collapsed }">
        <UserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <UDashboardSearch
      :groups="searchGroups"
      placeholder="Cari menu atau halaman..."
    />

    <slot />

    <NotificationsSlideover />
  </UDashboardGroup>
</template>
