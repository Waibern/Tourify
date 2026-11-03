// Pengaturan bisa dibuka admin dan petugas; halaman anggota hanya untuk admin.
export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) {
    if (to.path !== '/login') {
      return navigateTo('/login')
    }

    return
  }

  // Login dirender dulu agar layout tidak berubah saat hidrasi browser.
  if (useNuxtApp().isHydrating && to.path === '/login') {
    return
  }

  const auth = useDashboardAuth()
  // Refresh izin server di setiap pindah halaman supaya submenu ikut role terbaru.
  const user = await auth.loadSession()

  if (!user) {
    return to.path === '/login' ? undefined : navigateTo('/login')
  }

  if (to.path === '/login') {
    return navigateTo(user.role === 'gate' ? '/scanner' : '/')
  }

  const isSettingsRoute = to.path === '/settings' || to.path.startsWith('/settings/')
  if (user.role === 'gate' && isSettingsRoute && to.path !== '/settings/members') {
    return
  }

  if (user.role === 'gate' && to.path === '/settings/members') {
    return navigateTo('/settings')
  }

  if (user.role === 'gate' && to.path !== '/scanner') {
    return navigateTo('/scanner')
  }

  if (user.role === 'admin' && !['/', '/customers', '/scanner', '/wahana', '/orders', '/checkins'].includes(to.path) && !isSettingsRoute) {
    return navigateTo('/')
  }
})
