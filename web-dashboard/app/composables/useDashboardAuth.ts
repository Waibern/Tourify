import type { DashboardApiResponse, DashboardUser } from '~/types/tourify'

// Login, pemeriksaan halaman, dan menu akun memakai sesi yang sama.
export function useDashboardAuth() {
  const api = useTourifyApi()
  const user = useState<DashboardUser | null>('dashboard-user', () => null)

  // Ambil foto menu akun dari profil yang tersimpan di server.
  async function attachProfileImage(dashboardUser: DashboardUser): Promise<DashboardUser> {
    try {
      const response = await api.get<DashboardApiResponse>('profile.php')
      return { ...dashboardUser, profile_image: response.profile?.profile_image ?? null }
    } catch {
      return dashboardUser
    }
  }

  // Baca peran terbaru supaya menu langsung mengikuti izin akun.
  async function loadSession(): Promise<DashboardUser | null> {
    const cachedUser = user.value
    try {
      const response = await api.get<DashboardApiResponse>('dashboard_auth.php')
      if (!response.user) {
        user.value = null
      } else if (cachedUser?.id === response.user.id && cachedUser.profile_image !== undefined) {
        user.value = { ...response.user, profile_image: cachedUser.profile_image }
      } else {
        user.value = await attachProfileImage(response.user)
      }
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
