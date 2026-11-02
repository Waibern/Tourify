<script setup lang="ts">
import { sub } from 'date-fns'
import type { DashboardAnalytics, DashboardAnalyticsApiResponse } from '~/types/tourify'
import type { Period, Range } from '~/types'

const api = useTourifyApi()
// Tombol uji memakai notifikasi lokal agar animasi bisa diulang tanpa transaksi baru.
const { showTestNotification, showCustomerTestNotification, testActive, testLoading } = useDashboardNotifications()
// Dua jenis contoh berbagi satu menu dan tidak bisa ditumpuk saat tes masih berjalan.
const testNotificationItems = computed(() => [{
  label: 'Pesanan baru',
  icon: 'i-lucide-receipt-text',
  disabled: testActive.value || testLoading.value,
  onSelect: showTestNotification
}, {
  label: 'Akun baru',
  icon: 'i-lucide-user-round-plus',
  disabled: testActive.value || testLoading.value,
  onSelect: showCustomerTestNotification
}])
const range = shallowRef<Range>({
  start: sub(new Date(), { days: 13 }),
  end: new Date()
})
const period = ref<Period>('daily')
const analytics = ref<DashboardAnalytics | null>(null)
const loading = ref(true)
const errorMessage = ref('')
let requestId = 0

// Tanggal dikirim sebagai tanggal lokal supaya filter tetap sesuai kalender pengguna.
function toDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return year + '-' + month + '-' + day
}

// Polling menjaga data dan loading tetap utuh agar ASCII hanya berjalan saat muat biasa.
async function loadAnalytics(background = false) {
  const request = ++requestId
  if (!background) {
    loading.value = true
    errorMessage.value = ''
    analytics.value = null
  }

  const params = new URLSearchParams({
    start: toDateKey(range.value.start),
    end: toDateKey(range.value.end)
  })

  try {
    const response = await api.get<DashboardAnalyticsApiResponse>(
      'dashboard_analytics.php?' + params.toString()
    )
    if (request !== requestId) return
    if (!response.analytics) {
      throw new Error(response.message || 'Data analytics belum tersedia.')
    }

    // Respons yang sama tidak perlu menggambar ulang grafik setiap lima detik.
    if (JSON.stringify(analytics.value) !== JSON.stringify(response.analytics)) {
      analytics.value = response.analytics
    }
    errorMessage.value = ''
  } catch (error) {
    if (request === requestId) errorMessage.value = getTourifyErrorMessage(error, 'Analytics gagal dimuat.')
  } finally {
    if (request === requestId) loading.value = false
  }
}

onMounted(() => loadAnalytics())
useDashboardAutoRefresh(() => loadAnalytics(true), () => loading.value)
watch(
  () => toDateKey(range.value.start) + ':' + toDateKey(range.value.end),
  () => {
    void loadAnalytics()
  }
)
</script>

<template>
  <UDashboardPanel id="home">
    <template #header>
      <!-- Judul menyebut produk; filter tanggal di bawahnya tetap jadi konteks data. -->
      <UDashboardNavbar title="Aktivitas Tourify" :ui="{ right: 'gap-3' }">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
        <template #right>
          <!-- Pemicu tetap bisa menerima fokus balik; hanya pilihan yang sedang sibuk dinonaktifkan. -->
          <UDropdownMenu :items="testNotificationItems" :content="{ align: 'end' }" :ui="{ item: 'min-h-11' }">
            <UButton
              label="Tes notifikasi"
              :icon="testLoading ? 'i-lucide-loader-circle' : 'i-lucide-bell-ring'"
              trailing-icon="i-lucide-chevron-down"
              color="neutral"
              variant="ghost"
              aria-label="Tes notifikasi"
              title="Tes notifikasi"
              :aria-busy="testLoading"
              :ui="{ label: 'hidden min-[500px]:inline', leadingIcon: testLoading ? 'animate-spin' : '', trailingIcon: 'hidden min-[500px]:block' }"
              class="min-h-10 min-w-10"
            />
          </UDropdownMenu>
          <UButton
            label="Perbarui"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="ghost"
            :loading="loading"
            @click="loadAnalytics()"
          />
        </template>
      </UDashboardNavbar>

      <UDashboardToolbar>
        <template #left>
          <HomeDateRangePicker v-model="range" class="-ms-1" />
          <HomePeriodSelect v-model="period" :range="range" />
        </template>
      </UDashboardToolbar>
    </template>

    <template #body>
      <div class="space-y-4">
        <div v-if="errorMessage" class="flex flex-wrap items-start gap-3">
          <UAlert
            color="error"
            variant="subtle"
            :description="errorMessage"
            class="min-w-0 flex-1"
          />
          <UButton
            label="Coba lagi"
            icon="i-lucide-refresh-cw"
            color="neutral"
            variant="subtle"
            :loading="loading"
            @click="loadAnalytics()"
          />
        </div>

        <!-- Peringkat wahana mengikuti rentang tanggal analytics yang sedang dipilih. -->
        <HomeTopWahana
          v-if="analytics || loading"
          :wahana="analytics?.top_wahana ?? null"
          :loading="loading"
        />

        <HomeStats
          :stats="analytics?.stats ?? null"
          :previous="analytics?.previous_stats ?? null"
          :series="analytics?.series ?? []"
          :comparison="analytics?.comparison ?? null"
          :loading="loading"
        />
        <HomeChart
          :series="analytics?.series ?? []"
          :previous-value="analytics?.previous_stats.booking_value ?? null"
          :period="period"
          :loading="loading"
        />
        <HomeSales
          :orders="analytics?.recent_orders ?? []"
          :loading="loading"
        />
      </div>
    </template>
  </UDashboardPanel>
</template>
