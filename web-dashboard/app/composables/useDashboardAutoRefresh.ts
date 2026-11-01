// VueUse membersihkan timer saat halaman ditinggalkan; tab tersembunyi tidak mengirim request.
export function useDashboardAutoRefresh(refresh: () => Promise<void>, isBusy: () => boolean) {
  const visibility = useDocumentVisibility()
  let refreshing = false

  useIntervalFn(async () => {
    if (visibility.value !== 'visible' || refreshing || isBusy()) {
      return
    }

    // Tunggu request sebelumnya selesai supaya koneksi lambat tidak menumpuk pekerjaan.
    refreshing = true
    try {
      await refresh()
    } finally {
      refreshing = false
    }
  }, 5000)
}
