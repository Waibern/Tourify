import { h } from 'vue'
import DashboardActivityNotice from '~/components/DashboardActivityNotice.vue'
import type { DashboardActivityEvent, DashboardCustomersApiResponse } from '~/types/tourify'

// Notifikasi asli dan tombol tes memakai kartu serta siklus animasi yang sama.
export function useDashboardNotifications() {
  const toast = useToast()
  const api = useTourifyApi()
  const { user } = useDashboardAuth()
  const testId = 'dashboard-activity:test'
  const testActive = computed(() => toast.toasts.value.some(item => item.id === testId))
  const testLoading = ref(false)

  function notify(activity: DashboardActivityEvent, test = false) {
    const id = test ? testId : `dashboard-activity:${activity.id}`
    const notice = shallowRef<InstanceType<typeof DashboardActivityNotice> | null>(null)
    const paused = ref(false)
    // Escape dan swipe menunggu unreveal yang sama dengan tombol tutup.
    const requestDismiss = (event: Event) => {
      if (event.defaultPrevented) return
      event.preventDefault()
      if (notice.value) void notice.value.dismiss()
      else toast.remove(id)
    }
    toast.add({
      id,
      title: () => h(DashboardActivityNotice, { ref: notice, activity, test, paused: paused.value, onDismiss: () => toast.remove(id) }),
      color: 'neutral',
      type: 'background',
      // Timer tetap di isi kartu agar toast menunggu unreveal sebelum dilepas.
      duration: 0,
      progress: false,
      close: false,
      onPause: () => { paused.value = true },
      onResume: () => { paused.value = false },
      onEscapeKeyDown: requestDismiss,
      onSwipeEnd: requestDismiss,
      // Host tetap transparan; permukaan kartu dan sudutnya diungkap oleh isi notifikasi.
      class: 'dashboard-activity-toast overflow-visible rounded-xl bg-transparent p-0 ring-0 shadow-none',
      ui: { title: 'font-normal', actions: 'mt-0' }
    })
    return id
  }

  // Data contoh hanya ada di browser; klik berulang tidak menumpuk kartu yang sama.
  function showTestNotification() {
    if (testActive.value || testLoading.value) return
    return notify({
      id: 'test',
      entity_id: '0',
      type: 'order',
      username: 'Pelanggan contoh',
      occurred_at: new Date().toISOString(),
      wahana_name: 'Dunia Petualang',
      quantity: 4,
      total_price: 600000
    }, true)
  }

  // Ambil pelanggan yang sudah ada supaya tombol uji membuka akun asli tanpa membuat akun baru.
  async function showCustomerTestNotification() {
    if (testActive.value || testLoading.value) return
    const userId = user.value?.id
    if (!userId) return
    testLoading.value = true
    try {
      const response = await api.get<DashboardCustomersApiResponse>('dashboard_customers.php')
      if (user.value?.id !== userId || testActive.value) return
      const customer = response.customers?.[0]
      if (!customer) {
        toast.add({ title: 'Belum ada pelanggan untuk tes', description: 'Tes tombol Lihat pelanggan membutuhkan akun pelanggan yang sudah terdaftar.', color: 'warning' })
        return
      }
      return notify({
        id: 'test-customer',
        entity_id: String(customer.id),
        type: 'customer',
        username: customer.username,
        occurred_at: new Date().toISOString()
      }, true)
    } catch (error) {
      // Respons permintaan lama tidak boleh memunculkan pesan setelah pengguna keluar.
      if (user.value?.id === userId) {
        toast.add({ title: 'Tes akun baru gagal', description: getTourifyErrorMessage(error, 'Data pelanggan gagal dimuat. Coba lagi.'), color: 'error' })
      }
    } finally {
      testLoading.value = false
    }
  }

  return { notify, showTestNotification, showCustomerTestNotification, testActive, testLoading }
}
