// Semua request web ke PHP memakai sesi cookie dan URL yang bisa diganti lewat env.
export function useTourifyApi() {
  const config = useRuntimeConfig()
  const baseUrl = String(config.public.tourifyApiBaseUrl).replace(/\/+$/, '')

  function url(path: string): string {
    return `${baseUrl}/${path.replace(/^\/+/, '')}`
  }

  return {
    // Path media memakai base URL API yang sama dengan request PHP.
    assetUrl(path?: string | null) {
      return path ? url(path) : undefined
    },
    get<T>(path: string) {
      return $fetch<T>(url(path), { credentials: 'include' })
    },
    post<T>(path: string, body: Record<string, unknown>, options: { timeout?: number } = {}) {
      return $fetch<T>(url(path), { method: 'POST', body, credentials: 'include', ...options })
    },
    // FormData mengirim foto dan field tambahan tanpa menebak multipart boundary.
    upload<T>(path: string, file: File, fields: Record<string, string> = {}) {
      const body = new FormData()
      body.append('image', file)
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

// Pesan dari API ditampilkan apa adanya supaya UI tidak menebak hasil transaksi.
export function getTourifyErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'data' in error) {
    const data = error.data
    if (typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string') {
      return data.message
    }
  }

  return fallback
}
