import type { DashboardActivityApiResponse, DashboardActivityCursor } from '~/types/tourify'

// Monitor tinggal di layout agar berpindah halaman tidak mengulang patokan atau notifikasi.
export function useDashboardActivity() {
  const api = useTourifyApi()
  const { user } = useDashboardAuth()
  const toast = useToast()
  const { notify } = useDashboardNotifications()
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
        const id = notify(event)
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
    dismissActivity()
    if (import.meta.client) void poll()
  }, { immediate: true })
  useDashboardAutoRefresh(poll, () => busy || adminId.value === null)
  onBeforeUnmount(() => {
    disposed = true
    generation++
    dismissActivity()
  })
}
