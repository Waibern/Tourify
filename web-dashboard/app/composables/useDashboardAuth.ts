import type { DashboardApiResponse, DashboardUser } from '~/types/tourify'

// Sesi dashboard dipakai bareng oleh login, middleware, dan menu akun.
export function useDashboardAuth() {
  const api = useTourifyApi()
  const user = useState<DashboardUser | null>('dashboard-user', () => null)

  // Foto menu akun dimuat dari profil yang tersimpan di server.
  async function attachProfileImage(dashboardUser: DashboardUser): Promise<DashboardUser> {
    try {
      const response = await api.get<DashboardApiResponse>('profile.php')
      return { ...dashboardUser, profile_image: response.profile?.profile_image ?? null }
    } catch {
      return dashboardUser
    }
  }

  async function loadSession(): Promise<DashboardUser | null> {
    try {
      const response = await api.get<DashboardApiResponse>('dashboard_auth.php')
      user.value = response.user ? await attachProfileImage(response.user) : null
    } catch {
      user.value = null
    }

    return user.value
  }

  async function login(username: string, password: string): Promise<DashboardUser> {
    const response = await api.post<DashboardApiResponse>('dashboard_auth.php', { username, password })
    if (!response.user) {
      throw new Error(response.message || 'Login dashboard gagal.')
    }

    const dashboardUser = await attachProfileImage(response.user)
    user.value = dashboardUser
    return dashboardUser
  }

  async function logout(): Promise<void> {
    try {
      await api.delete<DashboardApiResponse>('dashboard_auth.php')
    } finally {
      user.value = null
      await navigateTo('/login')
    }
  }

  return { user, loadSession, login, logout }
}
