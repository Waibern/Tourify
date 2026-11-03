import { h } from 'vue'
import DashboardActivityNotice from '~/components/DashboardActivityNotice.vue'
import type { DashboardActivityEvent, DashboardCustomersApiResponse } from '~/types/tourify'

// Nomor urut berada di level modul supaya toast tetap unik setelah halaman dibuka ulang.
let successNoticeSequence = 0
let notificationAudioContext: AudioContext | null = null
let notificationAudioUnlockRegistered = false
let lastNotificationSoundAt = 0

// Browser membuka audio setelah ada interaksi pengguna supaya bunyi tidak diblokir.
function unlockNotificationSound() {
  if (typeof window === 'undefined' || !window.AudioContext) return

  try {
    notificationAudioContext ??= new window.AudioContext()
    if (notificationAudioContext.state === 'suspended') {
      void notificationAudioContext.resume().catch(() => undefined)
    }
  } catch {
    notificationAudioContext = null
  }

  document.removeEventListener('pointerdown', unlockNotificationSound)
  document.removeEventListener('keydown', unlockNotificationSound)
}

function prepareNotificationSound() {
  if (typeof window === 'undefined' || notificationAudioUnlockRegistered || !window.AudioContext) return

  notificationAudioUnlockRegistered = true
  document.addEventListener('pointerdown', unlockNotificationSound, { once: true, passive: true })
  document.addEventListener('keydown', unlockNotificationSound, { once: true })
}

function scheduleNotificationChime(context: AudioContext) {
  for (const [offset, frequency] of [[0, 880], [0.08, 1174]] as const) {
    const oscillator = context.createOscillator()
    const volume = context.createGain()
    const startAt = context.currentTime + offset
    const endAt = startAt + 0.18

    oscillator.type = 'sine'
    oscillator.frequency.setValueAtTime(frequency, startAt)
    volume.gain.setValueAtTime(0.0001, startAt)
    volume.gain.exponentialRampToValueAtTime(0.035, startAt + 0.012)
    volume.gain.exponentialRampToValueAtTime(0.0001, endAt)
    oscillator.connect(volume)
    volume.connect(context.destination)
    oscillator.addEventListener('ended', () => {
      oscillator.disconnect()
      volume.disconnect()
    }, { once: true })
    oscillator.start(startAt)
    oscillator.stop(endAt)
  }
}

// Notifikasi yang datang beruntun hanya membunyikan satu chime pendek.
function playNotificationSound(enabled: boolean) {
  const context = notificationAudioContext
  const now = Date.now()
  if (!enabled || !context || now - lastNotificationSoundAt < 700) return

  lastNotificationSoundAt = now
  if (context.state === 'suspended') {
    void context.resume()
      .then(() => {
        if (context.state === 'running') scheduleNotificationChime(context)
      })
      .catch(() => undefined)
    return
  }
  if (context.state !== 'running') return

  scheduleNotificationChime(context)
}

// Notifikasi asli dan tombol tes memakai kartu serta siklus animasi yang sama.
export function useDashboardNotifications() {
  prepareNotificationSound()
  const {
    notificationSound,
    desktopNotifications,
    newOrderNotifications,
    newCustomerNotifications
  } = useDashboardPreferences()
  const toast = useToast()
  const api = useTourifyApi()
  const { user } = useDashboardAuth()
  const testId = 'dashboard-activity:test'
  const testActive = computed(() => toast.toasts.value.some(item => item.id === testId))
  const testLoading = ref(false)

  // Satu wadah menjaga aktivitas dan pesan sukses memakai siklus ASCII yang sama.
  function addNotice(id: string, content: {
    activity?: DashboardActivityEvent
    title?: string
    description?: string
    icon?: string
    test?: boolean
  }) {
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
      title: () => h(DashboardActivityNotice, { ref: notice, ...content, paused: paused.value, onDismiss: () => toast.remove(id) }),
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
    playNotificationSound(notificationSound.value)
    return id
  }

  function notify(activity: DashboardActivityEvent, test = false) {
    const activityEnabled = activity.type === 'order'
      ? newOrderNotifications.value
      : newCustomerNotifications.value
    if (!activityEnabled) return

    const id = test ? testId : `dashboard-activity:${activity.id}`
    addNotice(id, { activity, test })
    if (!test && desktopNotifications.value) showDesktopNotification(activity)
    return id
  }

  // Notifikasi desktop hanya muncul saat dashboard terbuka dan izin browser aktif.
  function showDesktopNotification(activity: DashboardActivityEvent) {
    if (typeof window === 'undefined' || !('Notification' in window) || Notification.permission !== 'granted') return

    const isOrder = activity.type === 'order'
    const target = isOrder
      ? `/orders?ticket=${encodeURIComponent(activity.entity_id)}`
      : `/customers?customer=${encodeURIComponent(activity.entity_id)}`
    const title = isOrder ? `Pesanan baru #${activity.entity_id}` : 'Pelanggan baru'
    const body = isOrder
      ? `${activity.username} memesan ${activity.quantity} tiket ${activity.wahana_name}.`
      : `${activity.username} baru terdaftar di Tourify.`
    const notification = new Notification(title, {
      body,
      icon: '/logo-putih.svg',
      tag: `tourify-${activity.id}`
    })

    notification.onclick = () => {
      window.focus()
      void navigateTo(target)
      notification.close()
    }
  }

  // Simpan wahana memakai kartu sukses dan ASCII reveal yang sama dengan aktivitas.
  function notifySuccess(title: string, description: string) {
    return addNotice(`dashboard-wahana-save:${++successNoticeSequence}`, {
      title,
      description,
      icon: 'i-lucide-circle-check'
    })
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

  return { notify, notifySuccess, showTestNotification, showCustomerTestNotification, testActive, testLoading }
}
