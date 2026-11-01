<script setup lang="ts">
import { createAsciiReveal } from 'ascii-reveal/core'
import type { NavigationMenuItem } from '@nuxt/ui'

const auth = useDashboardAuth()
const route = useRoute()

// Aktivitas baru tetap terpantau di seluruh halaman admin.
const { newOrderCount, newCustomerCount } = useDashboardActivity()

const open = ref(false)
const navigationSearch = ref('')
const ghostHost = ref<HTMLElement | null>(null)
const reducedMotion = usePreferredReducedMotion()

// Hantu tampil sebentar setiap kali sidebar dibuka, lalu canvas-nya dibersihkan.
watch(ghostHost, (host, _previous, onCleanup) => {
  if (!host || reducedMotion.value === 'reduce') return

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

// Pencarian ini menyaring nama menu yang bisa dibuka role saat ini.
const visibleLinks = computed(() => {
  const query = navigationSearch.value.trim().toLocaleLowerCase('id-ID')
  if (!query) return links.value

  return links.value.filter(item =>
    (item.label ?? '').toLocaleLowerCase('id-ID').includes(query)
  )
})

// Pencarian dikosongkan setelah pindah halaman supaya semua menu muncul lagi.
function closeSidebar() {
  open.value = false
  navigationSearch.value = ''
}

// Tutup menu HP saat sidebar menetap agar overlay tidak menyisakan kunci fokus.
const sidebarVisible = useMediaQuery('(min-width: 768px)')
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
        <UInput
          v-if="!collapsed"
          v-model="navigationSearch"
          icon="i-lucide-search"
          placeholder="Cari menu"
          aria-label="Cari menu navigasi"
          size="sm"
          class="mb-3 mx-1"
        />

        <p
          v-if="!collapsed && navigationSearch.trim() && !visibleLinks.length"
          role="status"
          class="px-3 py-2 text-sm text-muted"
        >
          Menu tidak ditemukan.
        </p>

        <!-- Jarak antar tautan dibuat lega supaya navigasi mudah dipindai. -->
        <UNavigationMenu
          :collapsed="collapsed"
          :items="collapsed ? links : visibleLinks"
          orientation="vertical"
          :ui="{ list: 'space-y-1' }"
          tooltip
          popover
        />

        <div
          v-if="!collapsed"
          ref="ghostHost"
          aria-hidden="true"
          class="relative mt-8 h-36 shrink-0 overflow-hidden text-primary min-[768px]:h-80"
        />
      </template>

      <template #footer="{ collapsed }">
        <UserMenu :collapsed="collapsed" />
      </template>
    </UDashboardSidebar>

    <slot />
  </UDashboardGroup>
</template>
