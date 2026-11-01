// Nol, sebagian, dan penuh dibedakan supaya petugas tidak salah membaca kehadiran.
export function orderCheckinLabel(checkedIn: number, quantity: number): string {
  if (checkedIn === 0) return 'Belum masuk'
  return checkedIn >= quantity ? 'Semua masuk' : 'Sebagian masuk'
}
