import type { DashboardActivityApiResponse, DashboardActivityCursor } from '~/types/tourify'

export interface DashboardActivityNotification {
  activity: DashboardActivityApiResponse['events'][number]
  unread: boolean
}

// Panel lonceng memakai feed yang sama dengan notifikasi aktivitas langsung.
export function useDashboardActivityFeed() {
  return useState<DashboardActivityNotification[]>('tourify-dashboard-activity-feed', () => [])
}

// Monitor tinggal di layout agar berpindah halaman tidak mengulang patokan atau notifikasi.
export function useDashboardActivity() {
  const api = useTourifyApi()
  const { user } = useDashboardAuth()
  const route = useRoute()
  const toast = useToast()
  const { notify } = useDashboardNotifications()
  const notificationFeed = useDashboardActivityFeed()
  const newOrderCount = ref(0)
  const newCustomerCount = ref(0)
  let cursor: DashboardActivityCursor | null = null
  let busy = false
  let generation = 0
  let disposed = false
  const adminId = computed(() => user.value?.role === 'admin' ? user.value.id : null)

  function dismissActivity() {
    for (const item of toast.toasts.value) {
      if (String(item.id).startsWith('dashboard-activity:')) toast.remove(item.id)
    }
  }

  async function poll() {
    if (adminId.value === null || busy || disposed) return
    const requestGeneration = generation
    busy = true
    try {
      const params = cursor
        ? `?${new URLSearchParams({ after_order: cursor.order_id, after_customer: cursor.customer_id })}`
        : ''
      const response = await api.get<DashboardActivityApiResponse>(`dashboard_activity.php${params}`)
      if (disposed || requestGeneration !== generation || !response.success) return

      // Patokan baru disimpan setelah request berhasil; kegagalan jaringan dicoba lagi nanti.
      const previous = cursor
      cursor = response.cursor
      if (!previous) return
      const delivered = new Set<string>()
      for (const event of response.events) {
        const after = event.type === 'order' ? previous.order_id : previous.customer_id
        if (BigInt(event.entity_id) <= BigInt(after) || delivered.has(event.id)) continue
        delivered.add(event.id)
        // Badge hanya menghitung aktivitas baru pada menu yang belum dibuka.
        if (event.type === 'order' && route.path !== '/orders') newOrderCount.value++
        if (event.type === 'customer' && route.path !== '/customers') newCustomerCount.value++
        const id = notify(event)
        if (!id) continue
        notificationFeed.value.unshift({ activity: event, unread: true })
        if (notificationFeed.value.length > 30) notificationFeed.value.length = 30
        // Nuxt memasang toast pada tick berikutnya; batalkan juga antreannya bila akun sudah keluar.
        await nextTick()
        if (disposed || requestGeneration !== generation) {
          toast.remove(id)
          break
        }
      }
    } catch {
      // Gangguan sementara tidak menimpa data dashboard atau memunculkan toast error berulang.
    } finally {
      busy = false
    }
  }

  // Ganti akun membuang patokan lama dan respons yang masih berjalan.
  watch(adminId, () => {
    generation++
    cursor = null
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
  useDashboardAutoRefresh(poll, () => busy || adminId.value === null)
  onBeforeUnmount(() => {
    disposed = true
    generation++
    dismissActivity()
  })

  return { newOrderCount, newCustomerCount }
}
