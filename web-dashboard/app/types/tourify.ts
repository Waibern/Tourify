export type DashboardRole = 'admin' | 'gate'

// ID berbentuk teks agar penanda polling tetap sama dengan ID dari PHP.
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
  // Path foto akun ikut sesi supaya avatar menu bisa langsung tampil.
  profile_image?: string | null
}

export interface DashboardProfile {
  username: string
  profile_image: string | null
}

export interface TourifyWahana {
  id: number
  name: string
  description: string
  ticket_price: number
}

export interface CheckinTicket {
  id: string
  wahana_name: string
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

export interface CheckinApiResponse extends DashboardApiResponse {
  ticket?: CheckinTicket
}

export interface WahanaUpdateApiResponse {
  success: boolean
  message?: string
  wahana?: TourifyWahana
}

// Bentuk data analytics dan pelanggan mengikuti tabel yang benar-benar ada di API.
export interface DashboardAnalyticsStats {
  booking_value: number
  new_customers: number
  orders: number
  tickets: number
  checkins: number
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
}

export interface DashboardAnalytics {
  stats: DashboardAnalyticsStats
  previous_stats: DashboardAnalyticsStats
  comparison: { start: string, end: string, days: number }
  series: DashboardAnalyticsPoint[]
  recent_orders: DashboardRecentOrder[]
  top_wahana: DashboardTopWahana | null
  // Tiga wahana teratas untuk rincian chart di banner.
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
  // Path foto relatif supaya daftar pelanggan memakai base URL API yang sama.
  profile_image: string | null
  created_at: string
  order_count: number
  ticket_count: number
  booking_value: number
}

export interface DashboardCustomersApiResponse {
  success: boolean
  message?: string
  customers?: DashboardCustomer[]
}

// Daftar admin dibatasi per halaman oleh server.
export interface DashboardOrder extends DashboardRecentOrder {
  wahana_id: number
  checked_in_quantity: number
  unit_price: number
  payment_method: string
  status: 'Active' | 'Expire' | 'Used'
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
