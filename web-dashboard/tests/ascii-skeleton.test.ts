import assert from 'node:assert/strict'
import { asciiSkeletonHTML } from 'ascii-reveal/core'

// API core harus bisa dirender tanpa window, dengan pola stabil untuk SSR dan hydration.
for (const rows of [2, 3, 8]) {
  const options = { variant: 'dither' as const, rows, css: false }
  const html = asciiSkeletonHTML(options)
  assert.equal(html, asciiSkeletonHTML(options))
  assert.ok(html.includes('data-ascii-skeleton'))
  assert.ok(html.includes('aria-hidden="true"'))
  assert.ok(html.includes('data-ars-animate'))
  assert.ok(!html.includes('<style>'))
  assert.ok(!html.includes('<script'))
}
console.log('ASCII skeleton SSR and deterministic markup checks passed')
