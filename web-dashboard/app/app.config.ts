export default defineAppConfig({
  ui: {
    colors: {
      primary: 'emerald',
      neutral: 'zinc'
    },

    // Di layar kecil, menu samping jadi navigasi utama.
    dashboardSidebar: {
      slots: {
        root: 'md:flex md:max-lg:max-w-52',
        content: 'md:hidden',
        overlay: 'md:hidden'
      }
    },

    // Tombol menu dan sidebar berubah di ukuran layar yang sama.
    dashboardSidebarToggle: {
      base: 'md:hidden'
    },
    dashboardSidebarCollapse: {
      base: 'md:flex'
    }
  }
})
