// Profil bisa dibuka admin dan petugas; halaman demo lainnya tetap diarahkan.
export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) {
    if (to.path !== '/login') {
      return navigateTo('/login')
    }

    return
  }

  const auth = useDashboardAuth()
  const user = auth.user.value ?? await auth.loadSession()

  if (!user) {
    return to.path === '/login' ? undefined : navigateTo('/login')
  }

  if (to.path === '/login') {
    return navigateTo(user.role === 'gate' ? '/scanner' : '/')
  }

  if (user.role === 'gate' && !['/scanner', '/settings'].includes(to.path)) {
    return navigateTo('/scanner')
  }

  if (user.role === 'admin' && !['/', '/customers', '/scanner', '/wahana', '/orders', '/checkins', '/settings'].includes(to.path)) {
    return navigateTo('/')
  }
})
