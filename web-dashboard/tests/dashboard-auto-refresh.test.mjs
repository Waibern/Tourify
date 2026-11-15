import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from 'vue/compiler-sfc'
import { useDashboardAutoRefresh } from '../app/composables/useDashboardAutoRefresh.ts'

// Atur waktu di tes supaya jeda lima detik dan permintaan bersamaan bisa dicek cepat.
test('polling lima detik melewati tab tersembunyi, halaman sibuk, dan request aktif', async (t) => {
  let tick
  let interval
  let busy = false
  let finish
  let calls = 0
  const visibility = { value: 'visible' }
  globalThis.useDocumentVisibility = () => visibility
  globalThis.useIntervalFn = (callback, milliseconds) => {
    tick = callback
    interval = milliseconds
  }
  t.after(() => {
    delete globalThis.useDocumentVisibility
    delete globalThis.useIntervalFn
  })

  useDashboardAutoRefresh(() => {
    calls++
    return new Promise((resolve) => {
      finish = resolve
    })
  }, () => busy)
  assert.equal(interval, 5000)
  visibility.value = 'hidden'
  await tick()
  visibility.value = 'visible'
  busy = true
  await tick()
  assert.equal(calls, 0)

  busy = false
  const pending = tick()
  await tick()
  assert.equal(calls, 1)
  finish()
  await pending
  const next = tick()
  assert.equal(calls, 2)
  finish()
  await next
})

// Pakai pemuat asli dari halaman supaya tes menangkap masalah saat data dimuat.
const { descriptor } = parse(readFileSync(new URL('../app/pages/index.vue', import.meta.url), 'utf8'))
const source = ts.createSourceFile('index.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const loader = source.statements.find(node => ts.isFunctionDeclaration(node) && node.name?.text === 'loadAnalytics')
assert.ok(loader)
const loaderCode = ts.transpileModule(`${loader.getText(source)}\nloadAnalytics`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function createLoader(get) {
  const loadingChanges = []
  let loadingValue = false
  const scope = {
    requestId: 0,
    loading: {
      get value() { return loadingValue },
      set value(value) {
        loadingChanges.push(value)
        loadingValue = value
      }
    },
    analytics: { value: { orders: 1 } },
    errorMessage: { value: '' },
    range: { value: { start: '2026-10-19', end: '2026-11-01' } },
    toDateKey: value => value,
    URLSearchParams,
    getTourifyErrorMessage: (_error, fallback) => fallback,
    api: { get }
  }
  return { ...scope, loadingChanges, load: vm.runInNewContext(loaderCode, scope) }
}

test('data baru masuk tanpa mengosongkan kartu atau mengulang loading ASCII', async () => {
  let finish
  const state = createLoader(() => new Promise((resolve) => {
    finish = resolve
  }))
  const previous = state.analytics.value
  const pending = state.load(true)
  assert.equal(state.analytics.value, previous)
  assert.equal(state.loading.value, false)
  finish({ analytics: { orders: 2 } })
  await pending
  assert.equal(state.analytics.value.orders, 2)
  assert.ok(!state.loadingChanges.includes(true))

  const unchanged = state.analytics.value
  const next = state.load(true)
  finish({ analytics: { orders: 2 } })
  await next
  assert.equal(state.analytics.value, unchanged)
})

test('gagal polling mempertahankan data terakhir dan pulih pada pembaruan berikutnya', async () => {
  let offline = true
  const state = createLoader(async () => {
    if (offline) throw new Error('offline')
    return { analytics: { orders: 3 } }
  })
  await state.load(true)
  assert.equal(state.analytics.value.orders, 1)
  assert.ok(state.errorMessage.value)
  offline = false
  await state.load(true)
  assert.equal(state.analytics.value.orders, 3)
  assert.equal(state.errorMessage.value, '')
  assert.ok(!state.loadingChanges.includes(true))
})

test('respons polling lama tidak menimpa permintaan dari filter yang baru', async () => {
  const requests = []
  const state = createLoader(() => new Promise((resolve) => {
    requests.push(resolve)
  }))
  const background = state.load(true)
  const foreground = state.load()
  requests[1]({ analytics: { orders: 5 } })
  await foreground
  requests[0]({ analytics: { orders: 1 } })
  await background
  assert.equal(state.analytics.value.orders, 5)
  assert.equal(state.loading.value, false)
})
