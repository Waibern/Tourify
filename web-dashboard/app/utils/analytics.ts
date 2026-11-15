// Kalau nilai awal nol, persentasenya tidak bisa dihitung, jadi tandanya disembunyikan.
export function analyticsChange(current: number, previous: number): { label: string, color: 'success' | 'error' | 'neutral' } | null {
  if (previous === 0) {
    return current === 0 ? { label: '0%', color: 'neutral' } : null
  }
  const change = (current - previous) / previous * 100
  const label = new Intl.NumberFormat('id-ID', { maximumFractionDigits: 1, signDisplay: 'exceptZero' }).format(change) + '%'
  return { label, color: change > 0 ? 'success' : change < 0 ? 'error' : 'neutral' }
}
