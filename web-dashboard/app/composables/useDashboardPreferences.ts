export function useDashboardPreferences() {
  // Cookie menyimpan pilihan dashboard saat halaman dimuat ulang.
  const asciiAnimations = useCookie<boolean>('tourify-dashboard-ascii-animations', {
    default: () => true,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax'
  })
  const notificationSound = useCookie<boolean>('tourify-dashboard-notification-sound', {
    default: () => true,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax'
  })
  const desktopNotifications = useCookie<boolean>('tourify-dashboard-notification-desktop', {
    default: () => false,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax'
  })
  const newOrderNotifications = useCookie<boolean>('tourify-dashboard-notification-orders', {
    default: () => true,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax'
  })
  const newCustomerNotifications = useCookie<boolean>('tourify-dashboard-notification-customers', {
    default: () => true,
    maxAge: 60 * 60 * 24 * 365,
    sameSite: 'lax'
  })

  return { asciiAnimations, notificationSound, desktopNotifications, newOrderNotifications, newCustomerNotifications }
}
