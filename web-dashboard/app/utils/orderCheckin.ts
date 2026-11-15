// Bedakan tiket yang belum, sebagian, dan sudah dipakai supaya statusnya jelas.
export function orderCheckinLabel(checkedIn: number, quantity: number): string {
  if (checkedIn === 0) return 'Belum masuk'
  return checkedIn >= quantity ? 'Semua masuk' : 'Sebagian masuk'
}
