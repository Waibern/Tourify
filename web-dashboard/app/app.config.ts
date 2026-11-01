export default defineAppConfig({
  ui: {
    colors: {
      primary: 'emerald',
      neutral: 'zinc'
    },

    // Drawer tetap jadi navigasi utama sampai layar cukup lebar untuk sidebar.
    dashboardSidebar: {
      slots: {
        root: 'md:flex md:max-lg:max-w-52',
        content: 'md:hidden',
        overlay: 'md:hidden'
      }
    },

    // Tombol menu HP dan lipat manual mengikuti batas tampilan sidebar yang sama.
    dashboardSidebarToggle: {
      base: 'md:hidden'
    },
    dashboardSidebarCollapse: {
      base: 'md:flex'
    }
  }
})
