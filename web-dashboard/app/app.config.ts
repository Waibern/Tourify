export default defineAppConfig({
  ui: {
    colors: {
      primary: 'green',
      neutral: 'zinc'
    },

    // Sidebar menetap mulai 500 px, jadi jendela separuh layar tetap punya navigasi.
    dashboardSidebar: {
      slots: {
        root: 'min-[500px]:flex min-[500px]:max-lg:max-w-52',
        content: 'min-[500px]:hidden',
        overlay: 'min-[500px]:hidden'
      }
    },

    // Tombol menu HP dan lipat manual mengikuti batas tampilan sidebar yang sama.
    dashboardSidebarToggle: {
      base: 'min-[500px]:hidden'
    },
    dashboardSidebarCollapse: {
      base: 'min-[500px]:flex'
    }
  }
})
