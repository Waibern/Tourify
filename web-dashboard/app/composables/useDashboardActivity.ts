import type { DashboardActivityApiResponse, DashboardActivityCursor } from '~/types/tourify'

export interface DashboardActivityNotification {
  activity: DashboardActivityApiResponse['events'][number]
  unread: boolean
}

// Lonceng menampilkan notifikasi yang sama dengan aktivitas dashboard.
export function useDashboardActivityFeed() {
  return useState<DashboardActivityNotification[]>('tourify-dashboard-activity-feed', () => [])
}

// Pantau aktivitas dari layout supaya pindah halaman tidak mengulang hitungan.
export function useDashboardActivity() {
  const api = useTourifyApi()
  const { user } = useDashboardAuth()
  const route = useRoute()
  const toast = useToast()
  const { notify } = useDashboardNotifications()
  const notificationFeed = useDashboardActivityFeed()
  const newOrderCount = ref(0)
  const newCustomerCount = ref(0)
  let activityCursor: DashboardActivityCursor | null = null
  let isPolling = false
  let accountGeneration = 0
  let isDisposed = false
  const adminId = computed(() => user.value?.role === 'admin' ? user.value.id : null)

  function dismissActivity() {
    for (const item of toast.toasts.value) {
      if (String(item.id).startsWith('dashboard-activity:')) toast.remove(item.id)
    }
  }

  async function poll() {
    if (adminId.value === null || isPolling || isDisposed) return
    const requestGeneration = accountGeneration
    isPolling = true
    try {
      const params = activityCursor
        ? `?${new URLSearchParams({ after_order: activityCursor.order_id, after_customer: activityCursor.customer_id })}`
        : ''
      const response = await api.get<DashboardActivityApiResponse>(`dashboard_activity.php${params}`)
      if (isDisposed || requestGeneration !== accountGeneration || !response.success) return

      // Simpan penanda setelah permintaan berhasil supaya bisa dicoba lagi kalau jaringan bermasalah.
      const previousCursor = activityCursor
      activityCursor = response.cursor
      if (!previousCursor) return
      const deliveredEventIds = new Set<string>()
      for (const event of response.events) {
        const previousId = event.type === 'order' ? previousCursor.order_id : previousCursor.customer_id
        if (BigInt(event.entity_id) <= BigInt(previousId) || deliveredEventIds.has(event.id)) continue
        deliveredEventIds.add(event.id)
        // Hitung aktivitas yang belum dilihat pada halamannya.
        if (event.type === 'order' && route.path !== '/orders') newOrderCount.value++
        if (event.type === 'customer' && route.path !== '/customers') newCustomerCount.value++
        const id = notify(event)
        if (!id) continue
        notificationFeed.value.unshift({ activity: event, unread: true })
        if (notificationFeed.value.length > 30) notificationFeed.value.length = 30
        // Setelah notifikasi muncul, cek lagi apakah admin masih login.
        await nextTick()
        if (isDisposed || requestGeneration !== accountGeneration) {
          toast.remove(id)
          break
        }
      }
    } catch {
      // Gangguan sementara tidak menghapus data atau memunculkan pesan error terus-menerus.
    } finally {
      isPolling = false
    }
  }

  // Saat akun berubah, reset penanda dan abaikan permintaan yang masih berjalan.
  watch(adminId, () => {
    accountGeneration++
    activityCursor = null
    newOrderCount.value = 0
    newCustomerCount.value = 0
    notificationFeed.value = []
    dismissActivity()
    if (import.meta.client) void poll()
  }, { immediate: true })
  watch(() => route.path, (path) => {
    if (path === '/orders') newOrderCount.value = 0
    if (path === '/customers') newCustomerCount.value = 0
  }, { immediate: true })
  useDashboardAutoRefresh(poll, () => isPolling || adminId.value === null)
  onBeforeUnmount(() => {
    isDisposed = true
    accountGeneration++
    dismissActivity()
  })

  return { newOrderCount, newCustomerCount }
}
