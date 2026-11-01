import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from 'vue/compiler-sfc'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'

// Jalankan filter halaman asli agar ID dari notifikasi diuji tanpa data server palsu.
const { descriptor } = parse(readFileSync(new URL('../app/pages/customers.vue', import.meta.url), 'utf8'))
const source = ts.createSourceFile('customers.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const script = source.statements.filter(node => !ts.isImportDeclaration(node)).map(node => node.getText(source)).join('\n')
const code = ts.transpileModule(`${script}\n;({ customers, search, pagination, filteredCustomers, customerId })`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function customerPage(query = {}) {
  const scope = effectScope()
  const route = reactive({ query })
  const state = scope.run(() => vm.runInNewContext(code, {
    computed, nextTick, ref, watch,
    useRoute: () => route,
    useTourifyApi: () => ({}),
    resolveComponent: () => 'avatar',
    useTemplateRef: () => ref(null),
    onMounted: () => {},
    useDashboardAutoRefresh: () => {}
  }))
  state.customers.value = [6, 16, 160].map(id => ({ id, username: `pelanggan${id}`, created_at: '2026-11-01' }))
  return { ...state, route, stop: () => scope.stop() }
}

test('tautan notifikasi memilih ID persis dan bisa berpindah pelanggan pada halaman yang sama', async (t) => {
  const page = customerPage({ customer: '6' })
  t.after(page.stop)
  assert.deepEqual(page.filteredCustomers.value.map(row => row.id), [6])
  page.search.value = 'tidak cocok'
  page.pagination.value.pageIndex = 4
  page.route.query.customer = '16'
  await nextTick()
  assert.equal(page.search.value, '')
  assert.equal(page.pagination.value.pageIndex, 0)
  assert.deepEqual(page.filteredCustomers.value.map(row => row.id), [16])
  delete page.route.query.customer
  await nextTick()
  assert.equal(page.filteredCustomers.value.length, 3)
})

test('ID tidak ditemukan menghasilkan daftar kosong; parameter tidak valid tidak menyaring daftar', (t) => {
  const missing = customerPage({ customer: '99' })
  t.after(missing.stop)
  assert.equal(missing.filteredCustomers.value.length, 0)
  for (const customer of ['0', '-1', '6x', ['6', '16']]) {
    const page = customerPage({ customer })
    assert.equal(page.customerId.value, '')
    assert.equal(page.filteredCustomers.value.length, 3)
    page.stop()
  }
})
