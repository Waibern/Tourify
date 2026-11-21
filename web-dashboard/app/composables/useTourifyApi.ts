// Semua permintaan ke PHP memakai sesi login dan alamat API dari pengaturan.
export function useTourifyApi() {
  const config = useRuntimeConfig()
  const baseUrl = String(config.public.tourifyApiBaseUrl).replace(/\/+$/, '')

  function url(path: string): string {
    return `${baseUrl}/${path.replace(/^\/+/, '')}`
  }

  return {
    // Foto memakai alamat dasar API yang sama dengan permintaan lainnya.
    assetUrl(path?: string | null) {
      return path ? url(path) : undefined
    },
    get<T>(path: string) {
      return $fetch<T>(url(path), { credentials: 'include' })
    },
    post<T>(path: string, body: Record<string, unknown>, options: { timeout?: number } = {}) {
      return $fetch<T>(url(path), { method: 'POST', body, credentials: 'include', ...options })
    },
    // FormData bisa mengirim data saja atau sekaligus dengan foto.
    upload<T>(path: string, file: File | null, fields: Record<string, string> = {}) {
      const body = new FormData()
      if (file) body.append('image', file)
      for (const [name, value] of Object.entries(fields)) body.append(name, value)
      return $fetch<T>(url(path), { method: 'POST', body, credentials: 'include' })
    },
    put<T>(path: string, body: Record<string, unknown>) {
      return $fetch<T>(url(path), { method: 'PUT', body, credentials: 'include' })
    },
    delete<T>(path: string, body?: Record<string, unknown>) {
      return $fetch<T>(url(path), { method: 'DELETE', body, credentials: 'include' })
    }
  }
}

// Tampilkan pesan dari API supaya hasil transaksi tidak ditebak oleh halaman.
export function getTourifyErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'data' in error) {
    const data = error.data
    if (typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string') {
      return data.message
    }
  }

  return fallback
}
