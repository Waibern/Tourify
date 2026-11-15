import assert from 'node:assert/strict'
import { test } from 'node:test'
import { orderCheckinLabel } from '../app/utils/orderCheckin.ts'

// Tiket yang belum dipakai tidak boleh disebut sudah check-in.
test('label kehadiran membedakan belum, sebagian, dan seluruh pengunjung', () => {
  assert.equal(orderCheckinLabel(0, 2), 'Belum masuk')
  assert.equal(orderCheckinLabel(1, 2), 'Sebagian masuk')
  assert.equal(orderCheckinLabel(2, 2), 'Semua masuk')
})
