import assert from 'node:assert/strict'
import { analyticsChange } from '../app/utils/analytics.ts'

// Kalau nilai awal nol, tanda persentase disembunyikan; penurunan ke nol tetap dihitung.
assert.equal(analyticsChange(12, 0), null)
assert.deepEqual(analyticsChange(0, 0), { label: '0%', color: 'neutral' })
assert.deepEqual(analyticsChange(0, 12), { label: '-100%', color: 'error' })
assert.deepEqual(analyticsChange(12, 12), { label: '0%', color: 'neutral' })
assert.deepEqual(analyticsChange(13, 12), { label: '+8,3%', color: 'success' })
console.log('Analytics comparison checks passed')
