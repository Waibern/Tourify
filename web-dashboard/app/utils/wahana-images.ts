// Daftar foto ini dipakai di halaman wahana dan ringkasan check-in.
export const wahanaImages: Record<number, string> = {
  1: '/wahana/dunia_petualang.jpg',
  2: '/wahana/samudra_biru.jpg',
  3: '/wahana/pulau_air.jpg',
  4: '/wahana/pantai_mentari.jpg',
  5: '/wahana/taman_satwa_pesisir.jpg'
}

export function wahanaImage(id: number): string {
  return wahanaImages[id] ?? wahanaImages[1]!
}
