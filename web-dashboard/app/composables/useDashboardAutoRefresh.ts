// Hentikan timer saat halaman ditinggalkan; tab tersembunyi tidak mengirim permintaan.
export function useDashboardAutoRefresh(refresh: () => Promise<void>, isBusy: () => boolean) {
  const visibility = useDocumentVisibility()
  let refreshing = false

  useIntervalFn(async () => {
    if (visibility.value !== 'visible' || refreshing || isBusy()) {
      return
    }

    // Tunggu permintaan sebelumnya selesai supaya koneksi lambat tidak menumpuk permintaan.
    refreshing = true
    try {
      await refresh()
    } finally {
      refreshing = false
    }
  }, 5000)
}
