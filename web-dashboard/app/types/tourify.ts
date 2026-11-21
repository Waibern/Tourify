export type DashboardRole = 'admin' | 'gate'

// ID dibuat teks supaya bentuknya sama dengan yang dikirim PHP.
export interface DashboardActivityCursor {
  order_id: string
  customer_id: string
}

export type DashboardActivityEvent = {
  id: string
  entity_id: string
  username: string
  occurred_at: string
} & ({
  type: 'order'
  wahana_name: string
  quantity: number
  total_price: number
} | {
  type: 'customer'
})

export interface DashboardActivityApiResponse {
  success: boolean
  cursor: DashboardActivityCursor
  events: DashboardActivityEvent[]
}

export interface DashboardUser {
  id: number
  username: string
  role: DashboardRole
  // Sesi menyimpan alamat foto supaya avatar langsung muncul di menu.
  profile_image?: string | null
}

export type DashboardMember = DashboardUser

export interface DashboardProfile {
  username: string
  profile_image: string | null
}

export interface TourifyWahana {
  // Path foto relatif terhadap alamat API.
  image_path?: string | null
  id: number
  name: string
  description: string
  ticket_price: number
  average_rating: number | null
  rating_count: number
  // Ringkasan tiket terkait untuk peringatan saat wahana diarsipkan.
  ticket_count?: number
  ticket_owner_count?: number
  // Pilihan tunggal untuk banner unggulan di aplikasi.
  is_featured: boolean
}

export interface CheckinTicket {
  id: string
  wahana_name: string
  // Nama akun pemesan ditampilkan sebagai identitas pengunjung.
  username: string
  visit_date: string
  quantity: number
  checked_in_quantity: number
  remaining_quantity: number
}

export interface DashboardApiResponse {
  success: boolean
  message?: string
  user?: DashboardUser
  profile?: DashboardProfile
  wahana?: TourifyWahana[]
}

export interface DashboardMembersApiResponse extends DashboardApiResponse {
  members?: DashboardMember[]
  member?: DashboardMember
}

export interface CheckinApiResponse extends DashboardApiResponse {
  ticket?: CheckinTicket
}

export interface WahanaUpdateApiResponse {
  success: boolean
  message?: string
  wahana?: TourifyWahana
}

// Tipe ini mengikuti data pelanggan dan grafik yang dikirim API.
export interface DashboardAnalyticsStats {
  booking_value: number
  new_customers: number
  orders: number
  tickets: number
  checkins: number
  // Jumlah wahana diambil dari katalog, bukan dari rentang tanggal.
  wahana: number
}

export interface DashboardAnalyticsPoint {
  new_customers: number
  ticket_count: number
  checkin_count: number
  date: string
  order_count: number
  booking_value: number
}

export interface DashboardRecentOrder {
  id: string
  username: string
  wahana_name: string
  booking_date: string
  visit_date: string
  quantity: number
  total_price: number
}

export interface DashboardTopWahana {
  id: number
  name: string
  order_count: number
  ticket_count: number
  booking_value: number
  average_rating: number | null
  rating_count: number
}

export interface DashboardAnalytics {
  stats: DashboardAnalyticsStats
  previous_stats: DashboardAnalyticsStats
  comparison: { start: string, end: string, days: number }
  series: DashboardAnalyticsPoint[]
  recent_orders: DashboardRecentOrder[]
  top_wahana: DashboardTopWahana | null
  // Tiga wahana teratas untuk grafik di banner.
  top_wahana_distribution: DashboardTopWahana[]
}

export interface DashboardAnalyticsApiResponse {
  success: boolean
  message?: string
  analytics?: DashboardAnalytics
}

export interface DashboardCustomer {
  id: number
  username: string
  // Foto memakai alamat relatif supaya URL API tetap sama.
  profile_image: string | null
  created_at: string
  order_count: number
  ticket_count: number
  booking_value: number
}

export interface DashboardCustomersApiResponse {
  success: boolean
  message?: string
  online_count: number
  customers?: DashboardCustomer[]
}

// Server mengirim daftar admin per halaman.
export interface DashboardOrder extends DashboardRecentOrder {
  wahana_id: number
  checked_in_quantity: number
  unit_price: number
  payment_method: string
  status: 'Active' | 'Expire' | 'Used'
  can_delete: boolean
}

// Grafik menampilkan jumlah pesanan harian selama 14 hari terakhir.
export interface DashboardOrderTrendPoint {
  date: string
  order_count: number
}

export interface DashboardCheckin {
  id: string
  ticket_id: string
  username: string
  wahana_name: string
  scanned_at: string
  staff_username: string
}

export interface DashboardOrdersApiResponse {
  success: boolean
  orders: DashboardOrder[]
  order_trend: DashboardOrderTrendPoint[]
  total: number
  page: number
  per_page: number
  stats: {
    orders: number
    tickets: number
    booking_value: number
  }
}

export interface DashboardCheckinsApiResponse {
  success: boolean
  checkins: DashboardCheckin[]
  total: number
  page: number
  per_page: number
  stats: {
    checkins: number
    tickets: number
    top_wahana_id: number | null
    top_wahana: string | null
    top_wahana_checkins: number
  }
}
