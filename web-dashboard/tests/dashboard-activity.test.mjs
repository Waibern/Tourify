import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { computed, effectScope, nextTick, ref, shallowRef, watch } from 'vue'

// Jalankan composable asli dengan API dan toast terkendali, tanpa menambah data ke server.
function compileComposable(name) {
  const source = ts.createSourceFile(`${name}.ts`, readFileSync(new URL(`../app/composables/${name}.ts`, import.meta.url), 'utf8'), ts.ScriptTarget.Latest, true)
  const owner = source.statements.find(node => ts.isFunctionDeclaration(node) && node.name?.text === name)
  return ts.transpileModule(`${owner.getText(source).replace('export ', '').replace('import.meta.client', 'true')}\n${name}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
}
const code = compileComposable('useDashboardActivity')
const notificationCode = compileComposable('useDashboardNotifications')
const baseline = { order_id: '10', customer_id: '5' }
const order = { id: 'order:11', entity_id: '11', type: 'order', username: 'pelanggan', occurred_at: '2026-11-01 11:00:00', wahana_name: 'Dunia Petualang', quantity: 2, total_price: 300000 }
const customer = { id: 'customer:6', entity_id: '6', type: 'customer', username: 'baru', occurred_at: '2026-11-01 11:00:00' }
const flush = () => new Promise(resolve => setImmediate(resolve))

function monitor(get, role = 'admin', enqueue) {
  const user = ref({ id: 1, role })
  const notificationFeed = ref([])
  const toasts = ref([{ id: 'ordinary-toast' }])
  const calls = []
  let tick
  let dispose
  let notifications
  const scope = effectScope()
  const context = {
    computed, watch, nextTick, ref, shallowRef, URLSearchParams, BigInt, Set,
    h: (component, props) => ({ component, props }),
    DashboardActivityNotice: {},
    prepareNotificationSound: () => {},
    playNotificationSound: () => {},
    useTourifyApi: () => ({ get: (path) => {
      calls.push(path)
      return get(path)
    } }),
    useDashboardAuth: () => ({ user }),
    useDashboardPreferences: () => ({
      notificationSound: ref(false),
      desktopNotifications: ref(false),
      newOrderNotifications: ref(true),
      newCustomerNotifications: ref(true)
    }),
    useRoute: () => ({ path: '/' }),
    useDashboardActivityFeed: () => notificationFeed,
    getTourifyErrorMessage: (_error, fallback) => fallback,
    useToast: () => ({
      toasts,
      add: toast => enqueue ? enqueue({ toast, toasts, user }) : toasts.value.push(toast),
      remove: id => toasts.value = toasts.value.filter(toast => toast.id !== id)
    }),
    useDashboardAutoRefresh: callback => tick = callback,
    onBeforeUnmount: callback => dispose = callback
  }
  context.useDashboardNotifications = () => {
    notifications = vm.runInNewContext(notificationCode, context)()
    return notifications
  }
  scope.run(() => vm.runInNewContext(code, context)())
  return {
    user,
    notificationFeed,
    toasts,
    calls,
    notifications,
    tick: () => tick(),
    stop: () => {
      dispose()
      scope.stop()
    }
  }
}

test('patokan pertama diam; aktivitas berikutnya muncul sekali dengan tautan yang bisa ditutup', async (t) => {
  const replies = [
    { success: true, cursor: baseline, events: [order] },
    { success: true, cursor: { order_id: '11', customer_id: '6' }, events: [order, customer, order] },
    { success: true, cursor: { order_id: '11', customer_id: '6' }, events: [order, customer] }
  ]
  const state = monitor(async () => replies.shift())
  t.after(state.stop)
  await flush()
  assert.equal(state.toasts.value.length, 1)
  await state.tick()
  assert.equal(state.calls[1], 'dashboard_activity.php?after_order=10&after_customer=5')
  assert.deepEqual(state.toasts.value.map(toast => toast.id), ['ordinary-toast', 'dashboard-activity:order:11', 'dashboard-activity:customer:6'])
  assert.equal(state.notificationFeed.value.map(item => item.activity.id).join(','), 'customer:6,order:11')
  assert.ok(state.notificationFeed.value.every(item => item.unread))
  assert.equal(state.toasts.value[1].duration, 0)
  assert.equal(state.toasts.value[1].close, false)
  const content = state.toasts.value[1].title()
  assert.equal(content.props.activity.quantity, 2)
  state.toasts.value[1].onPause()
  assert.equal(state.toasts.value[1].title().props.paused, true)
  state.toasts.value[1].onResume()
  assert.equal(state.toasts.value[1].title().props.paused, false)
  let dismissalRequested = 0
  content.props.ref.value = {
    dismiss: () => { dismissalRequested++ }
  }
  const escape = new Event('keydown', { cancelable: true })
  state.toasts.value[1].onEscapeKeyDown(escape)
  assert.ok(escape.defaultPrevented)
  assert.equal(dismissalRequested, 1)
  assert.equal(state.toasts.value.length, 3)
  await state.tick()
  assert.equal(state.toasts.value.length, 3)
  content.props.onDismiss()
  assert.equal(state.toasts.value.length, 2)
})

test('koneksi gagal tidak memajukan penanda; respons lama diabaikan setelah ganti akun', async (t) => {
  let step = 0
  let finish
  const state = monitor(async () => {
    step++
    if (step === 1) return { success: true, cursor: baseline, events: [] }
    if (step === 2) throw new Error('offline')
    if (step === 3) return new Promise(resolve => finish = resolve)
    return { success: true, cursor: baseline, events: [] }
  })
  t.after(state.stop)
  await flush()
  await state.tick()
  const pending = state.tick()
  await state.tick()
  assert.equal(state.calls.length, 3)
  assert.equal(state.calls[1], state.calls[2])
  state.user.value = { id: 2, role: 'admin' }
  await flush()
  finish({ success: true, cursor: { order_id: '11', customer_id: '5' }, events: [order] })
  await pending
  assert.equal(state.toasts.value.length, 1)
  await state.tick()
  assert.equal(state.calls[3], 'dashboard_activity.php')
})

test('gate tidak polling; logout dan unmount membersihkan kartu aktivitas saja', async () => {
  const state = monitor(async () => ({ success: true, cursor: baseline, events: [] }), 'gate')
  await state.tick()
  assert.equal(state.calls.length, 0)
  state.user.value = { id: 1, role: 'admin' }
  await flush()
  assert.equal(state.calls.length, 1)
  state.toasts.value.push({ id: 'dashboard-activity:order:11' })
  state.user.value = null
  await flush()
  assert.deepEqual(state.toasts.value.map(toast => toast.id), ['ordinary-toast'])
  state.stop()
  await state.tick()
  assert.equal(state.calls.length, 1)
})

test('toast yang masih mengantre tidak muncul lagi setelah logout', async (t) => {
  let first = true
  const state = monitor(async () => {
    if (first) {
      first = false
      return { success: true, cursor: baseline, events: [] }
    }
    return { success: true, cursor: { order_id: '11', customer_id: '6' }, events: [order, customer] }
  }, 'admin', ({ toast, toasts, user }) => {
    // Modelkan antrean Nuxt yang baru menambah toast setelah flush perubahan akun.
    user.value = null
    void nextTick(() => toasts.value.push(toast))
  })
  t.after(state.stop)
  await flush()
  await state.tick()
  assert.deepEqual(state.toasts.value.map(toast => toast.id), ['ordinary-toast'])
})

test('tombol tes menampilkan contoh lokal sekali dan bisa diulang setelah kartu ditutup', async (t) => {
  const state = monitor(async () => {
    throw new Error('API tidak diperlukan untuk tes notifikasi')
  }, 'gate')
  t.after(state.stop)
  const { showTestNotification, testActive } = state.notifications
  showTestNotification()
  assert.equal(state.calls.length, 0)
  assert.equal(testActive.value, true)
  const content = state.toasts.value[1].title()
  assert.equal(content.props.test, true)
  assert.equal(content.props.activity.username, 'Pelanggan contoh')
  showTestNotification()
  assert.equal(state.toasts.value.length, 2)
  content.props.onDismiss()
  assert.equal(testActive.value, false)
  showTestNotification()
  assert.equal(state.toasts.value.length, 2)
})

test('tes akun baru membaca pelanggan yang ada dan meneruskan ID asli ke kartu uji', async (t) => {
  const state = monitor(async () => ({ success: true, customers: [{ id: 8, username: 'akun_tersedia' }] }), 'gate')
  t.after(state.stop)
  const { showCustomerTestNotification, testActive, testLoading } = state.notifications
  await showCustomerTestNotification()
  assert.deepEqual(state.calls, ['dashboard_customers.php'])
  assert.equal(testLoading.value, false)
  assert.equal(testActive.value, true)
  const content = state.toasts.value[1].title()
  assert.equal(content.props.test, true)
  assert.equal(content.props.activity.type, 'customer')
  assert.equal(content.props.activity.entity_id, '8')
  assert.equal(content.props.activity.username, 'akun_tersedia')
  await showCustomerTestNotification()
  assert.equal(state.calls.length, 1)
  assert.equal(state.toasts.value.length, 2)
})

test('permintaan tes yang sedang berjalan menahan klik ganda dan diabaikan setelah logout', async (t) => {
  let finish
  const state = monitor(() => new Promise(resolve => finish = resolve), 'gate')
  t.after(state.stop)
  const { showCustomerTestNotification, showTestNotification, testLoading } = state.notifications
  const pending = showCustomerTestNotification()
  assert.equal(testLoading.value, true)
  await showCustomerTestNotification()
  showTestNotification()
  assert.equal(state.calls.length, 1)
  assert.equal(state.toasts.value.length, 1)
  state.user.value = null
  await flush()
  finish({ success: true, customers: [{ id: 8, username: 'akun_tersedia' }] })
  await pending
  assert.equal(testLoading.value, false)
  assert.equal(state.toasts.value.length, 1)
})

test('pelanggan kosong atau gagal dimuat memberi pesan dan tes bisa dicoba lagi', async () => {
  const empty = monitor(async () => ({ success: true, customers: [] }), 'gate')
  await empty.notifications.showCustomerTestNotification()
  assert.equal(empty.toasts.value[1].title, 'Belum ada pelanggan untuk tes')
  assert.equal(empty.notifications.testActive.value, false)
  assert.equal(empty.notifications.testLoading.value, false)
  empty.stop()

  const failed = monitor(async () => {
    throw new Error('offline')
  }, 'gate')
  await failed.notifications.showCustomerTestNotification()
  assert.equal(failed.toasts.value[1].title, 'Tes akun baru gagal')
  assert.equal(failed.notifications.testLoading.value, false)
  await failed.notifications.showCustomerTestNotification()
  assert.equal(failed.calls.length, 2)
  failed.stop()
})
