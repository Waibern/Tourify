<script setup lang="ts">
import { createAsciiReveal } from 'ascii-reveal/core'
import type { NavigationMenuItem } from '@nuxt/ui'

const auth = useDashboardAuth()
const route = useRoute()

// Aktivitas baru tetap terpantau di seluruh halaman admin.
const { newOrderCount, newCustomerCount } = useDashboardActivity()

const open = ref(false)
const ghostHost = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()
const { asciiAnimations } = useDashboardPreferences()

// Hantu tampil sebentar setiap kali sidebar dibuka, lalu canvas-nya dibersihkan.
watch([ghostHost, reducedMotion, asciiAnimations], ([host, motion, animations], _previous, onCleanup) => {
  if (!host || motion === 'reduce' || !animations) return

  const reveal = createAsciiReveal(host, { mascot: '/logo-putih.svg' })
  let destroyed = false
  let destroyTimer: ReturnType<typeof window.setTimeout> | undefined
  const destroyReveal = () => {
    if (destroyed) return
    destroyed = true
    reveal.destroy()
  }

  reveal.shimmer(true)
  const hideTimer = window.setTimeout(() => {
    reveal.shimmer(false)
    destroyTimer = window.setTimeout(destroyReveal, 300)
  }, 1600)

  onCleanup(() => {
    window.clearTimeout(hideTimer)
    if (destroyTimer) window.clearTimeout(destroyTimer)
    destroyReveal()
  })
}, { flush: 'post' })

// Menu samping ditutup setelah halaman dipilih.
function closeSidebar() {
  open.value = false
}

// Tutup menu HP saat sidebar menetap agar overlay tidak menyisakan kunci fokus.
const sidebarVisible = useMediaQuery('(min-width: 768px)')
watch(sidebarVisible, (visible) => {
  if (visible) {
    open.value = false
  }
})

// Tiap halaman pengaturan punya tautan sendiri seperti di template dashboard.
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

// Menu utama hanya menampilkan halaman yang bisa dibuka role saat ini.
const links = computed<NavigationMenuItem[]>(() => {
  const isAdmin = auth.user.value?.role === 'admin'
  const primary: NavigationMenuItem[] = []

  if (isAdmin) {
    primary.push({
      label: 'Overview',
      icon: 'i-lucide-chart-no-axes-combined',
      to: '/',
      // Isi aktif dan lapisan hover memakai bentuk kapsul yang sama.
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

  // Ikon gear mengikuti template; pengaturan dibuka sebagai daftar halaman.
  primary.push({
    label: 'Pengaturan',
    icon: 'i-lucide-settings',
    to: '/settings',
    type: 'trigger',
    defaultOpen: true,
    children: settingsLinks.value
  })

  // Nama tautan tetap terbaca saat sidebar hanya menampilkan ikon.
  return primary.map(item => ({
    ...item,
    ...{ 'aria-label': item.label },
    children: item.children?.map(child => ({
      ...child,
      ...{ 'aria-label': child.label }
    }))
  }))
})

// Palette memakai menu yang tersedia untuk role saat ini.
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

        <!-- Jarak antar tautan dibuat lega supaya navigasi mudah dipindai. -->
        <UNavigationMenu
          :collapsed="collapsed"
          :items="links"
          orientation="vertical"
          :ui="{ list: 'space-y-1', linkLabel: 'whitespace-normal break-words' }"
          tooltip
          popover
        />

        <div
          v-if="!collapsed && asciiAnimations"
          ref="ghostHost"
          aria-hidden="true"
          class="relative mt-8 h-36 shrink-0 overflow-hidden text-primary min-[768px]:h-80"
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
