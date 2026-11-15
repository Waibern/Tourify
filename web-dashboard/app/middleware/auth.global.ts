// Admin dan petugas boleh membuka pengaturan; menu anggota khusus admin.
export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) {
    if (to.path !== '/login') {
      return navigateTo('/login')
    }

    return
  }

  // Tampilkan login dulu supaya tampilannya tidak berubah saat halaman dimuat.
  if (useNuxtApp().isHydrating && to.path === '/login') {
    return
  }

  const auth = useDashboardAuth()
  // Cek izin lagi setiap pindah halaman supaya menu mengikuti peran terbaru.
  const user = await auth.loadSession()

  if (!user) {
    return to.path === '/login' ? undefined : navigateTo('/login')
  }

  if (to.path === '/login') {
    return navigateTo(user.role === 'gate' ? '/scanner' : '/')
  }

  const isSettingsRoute = to.path === '/settings' || to.path.startsWith('/settings/')
  // Petugas boleh membuka pengaturan umum, tapi tidak bisa membuka menu anggota.
  if (user.role === 'gate') {
    if (to.path === '/settings/members') {
      return navigateTo('/settings')
    }

    if (isSettingsRoute) {
      return
    }

    if (to.path !== '/scanner') {
      return navigateTo('/scanner')
    }

    return
  }

  const adminPages = ['/', '/customers', '/scanner', '/wahana', '/orders', '/checkins']
  const canOpenAdminPage = adminPages.includes(to.path) || isSettingsRoute
  if (!canOpenAdminPage) {
    return navigateTo('/')
  }
})
