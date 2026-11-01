import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'
import { parse } from 'vue/compiler-sfc'
import { computed, effectScope, markRaw, nextTick, reactive, ref, watch } from 'vue'

// Pakai skrip kartu asli agar urutan hide → dismiss dan timer bisa dibuktikan tanpa browser.
const { descriptor } = parse(readFileSync(new URL('../app/components/DashboardActivityNotice.vue', import.meta.url), 'utf8'))
const source = ts.createSourceFile('notice.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const script = source.statements.filter(node => !ts.isImportDeclaration(node)).map(node => node.getText(source)).join('\n')
const code = ts.transpileModule(`${script}\n;({ phase, remaining, closing, root, surface, dismiss, target })`, {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const flush = () => new Promise(resolve => setImmediate(resolve))

function notice({ motion = 'no-preference', holdEntrance = false, failHide = false } = {}) {
  const scope = effectScope()
  const props = reactive({ activity: { type: 'customer', entity_id: '6' }, paused: false })
  const timers = []
  const effects = []
  const events = []
  let mounted
  let unmount
  let now = 1000
  // Model fokus kecil ini menguji pemulihan tanpa meniru layout atau animasi browser.
  const document = { activeElement: null, body: null }
  const focusListeners = []
  const motionPreference = ref(motion)
  class FocusTarget {
    isConnected = true
    disabled = false
    constructor() { markRaw(this) }
    contains(target) { return target === this }
    matches() { return this.disabled }
    focus() { document.activeElement = this }
  }
  const trigger = new FocusTarget()
  const body = new FocusTarget()
  const fallback = new FocusTarget()
  const closeButton = new FocusTarget()
  const content = new FocusTarget()
  const host = new FocusTarget()
  const viewport = new FocusTarget()
  const siblings = []
  content.contains = target => target === content || target === closeButton
  host.contains = target => target === host || content.contains(target)
  host.parentElement = viewport
  viewport.contains = target => target === viewport || host.contains(target) || siblings.some(item => item.contains(target))
  viewport.querySelectorAll = () => [host, ...siblings]
  document.body = body
  document.activeElement = trigger
  document.querySelector = () => fallback
  const context = {
    computed, nextTick, ref, watch,
    document,
    HTMLElement: FocusTarget,
    Date: { now: () => now },
    defineProps: () => props,
    withDefaults: values => values,
    defineEmits: () => event => events.push(event),
    defineExpose: () => {},
    usePreferredReducedMotion: () => motionPreference,
    useEventListener: (_name, listener) => focusListeners.push(listener),
    onMounted: callback => mounted = callback,
    onBeforeUnmount: callback => unmount = callback,
    useTimeoutFn: (callback, delay) => {
      const timer = { callback, delay, running: false }
      timers.push(timer)
      return {
        start: () => { timer.running = true },
        stop: () => { timer.running = false }
      }
    },
    createAsciiReveal: (target, options) => {
      let finishReveal
      const reveal = new Promise(resolve => finishReveal = resolve)
      // Pisahkan penyelesaian glyph dan surface agar tes membuktikan urutan kedua tahap.
      const hides = []
      const effect = {
        target,
        options,
        destroyed: false,
        reveal: (runOptions) => {
          effect.within = runOptions?.within
          return holdEntrance ? reveal : Promise.resolve()
        },
        hide: (runOptions) => {
          if (failHide) return Promise.reject(new Error('canvas unavailable'))
          return new Promise(resolve => hides.push({ within: runOptions?.within, finish: resolve }))
        },
        destroy: () => { effect.destroyed = true },
        hides,
        finishHide: () => hides[0].finish(),
        finishSurface: () => hides[1].finish(),
        finishReveal
      }
      effects.push(effect)
      return effect
    }
  }
  const state = scope.run(() => vm.runInNewContext(code, context))
  state.root.value = content
  state.surface.value = { closest: () => host }
  mounted()
  return {
    ...state, props, timers, effects, events, host,
    motion: motionPreference,
    focus: { trigger, body, fallback, closeButton, viewport },
    get activeFocus() { return document.activeElement },
    setFocus: (target) => {
      document.activeElement = target
      focusListeners.forEach(listener => listener({ target }))
    },
    newFocusTarget: () => new FocusTarget(),
    addNotice: () => {
      const sibling = new FocusTarget()
      sibling.querySelector = () => ({})
      siblings.push(sibling)
      return sibling
    },
    setTime: value => now = value,
    enter: () => timers[1].callback(),
    stop: () => {
      unmount()
      scope.stop()
    }
  }
}

async function finishExit(card, pending) {
  card.effects[1].finishHide()
  await flush()
  card.effects[1].finishSurface()
  await pending
}

test('tiga detik utuh baru berjalan setelah reveal selesai, termasuk pause selama reveal', async (t) => {
  const card = notice({ holdEntrance: true })
  t.after(card.stop)
  assert.equal(card.timers[0].running, false)
  card.setTime(7000)
  const entrance = card.enter()
  await flush()
  card.props.paused = true
  await nextTick()
  assert.equal(card.remaining.value, 3000)
  assert.equal(card.timers[0].running, false)
  card.effects[0].finishReveal()
  await entrance
  assert.equal(card.phase.value, 'idle')
  assert.equal(card.timers[0].running, false)
  card.setTime(8000)
  card.props.paused = false
  await nextTick()
  assert.equal(card.timers[0].running, true)
  card.setTime(9000)
  card.props.paused = true
  await nextTick()
  assert.equal(card.remaining.value, 2000)
})

test('tautan akun baru membawa ID pelanggan yang tepat', (t) => {
  const card = notice()
  t.after(card.stop)
  assert.equal(card.target.value, '/customers?customer=6')
  card.props.activity = { type: 'order', entity_id: '19' }
  assert.equal(card.target.value, '/orders?ticket=19')
})

test('preferensi reduce motion yang berubah sebelum entrance tetap memulai timer baca', async (t) => {
  const card = notice()
  t.after(card.stop)
  card.motion.value = 'reduce'
  await card.enter()
  assert.equal(card.phase.value, 'idle')
  assert.equal(card.timers[0].running, true)
  assert.equal(card.effects.length, 0)
})

test('Escape memulihkan kontrol terakhir sebelum F8 setelah seluruh unreveal selesai', async (t) => {
  const card = notice()
  t.after(card.stop)
  await card.enter()
  const previousControl = card.newFocusTarget()
  card.setFocus(previousControl)
  card.setFocus(card.focus.viewport)
  card.setFocus(card.focus.closeButton)
  const pending = card.dismiss()
  card.setFocus(card.focus.body)
  await finishExit(card, pending)
  assert.equal(card.activeFocus, previousControl)
})

test('kartu berikutnya menerima fokus; pemicu yang hilang memakai tujuan cadangan', async () => {
  const stacked = notice()
  await stacked.enter()
  const nextNotice = stacked.addNotice()
  stacked.setFocus(stacked.focus.closeButton)
  const stackedExit = stacked.dismiss()
  stacked.setFocus(stacked.focus.body)
  await finishExit(stacked, stackedExit)
  assert.equal(stacked.activeFocus, nextNotice)
  stacked.stop()

  const removed = notice()
  await removed.enter()
  removed.focus.trigger.isConnected = false
  removed.setFocus(removed.focus.closeButton)
  const removedExit = removed.dismiss()
  removed.setFocus(removed.focus.body)
  await finishExit(removed, removedExit)
  assert.equal(removed.activeFocus, removed.focus.fallback)
  removed.stop()
})

test('penutupan otomatis dan perpindahan fokus saat unreveal tidak merebut fokus kerja', async () => {
  const automatic = notice()
  await automatic.enter()
  await finishExit(automatic, automatic.dismiss())
  assert.equal(automatic.activeFocus, automatic.focus.trigger)
  automatic.stop()

  const moved = notice()
  await moved.enter()
  moved.setFocus(moved.focus.closeButton)
  const pending = moved.dismiss()
  const newControl = moved.newFocusTarget()
  moved.setFocus(newControl)
  await finishExit(moved, pending)
  assert.equal(moved.activeFocus, newControl)
  moved.stop()
})

test('penutupan menunggu surface dan sudut selesai unreveal, klik ganda hanya menutup satu kali', async (t) => {
  const card = notice()
  t.after(card.stop)
  await card.enter()
  assert.equal(card.phase.value, 'idle')
  const pending = card.dismiss()
  assert.equal(card.phase.value, 'unrevealing')
  assert.equal(card.effects[1].target, card.host)
  assert.equal(card.effects[1].options.surfaces, '[data-notification-card]')
  assert.equal(card.effects[1].hides[0].within, card.root.value)
  assert.equal(card.events.length, 0)
  await card.dismiss()
  assert.equal(card.effects.length, 2)
  card.effects[1].finishHide()
  await flush()
  assert.equal(card.phase.value, 'unrevealing-surface')
  assert.equal(card.effects[1].hides[1].within, card.surface.value)
  assert.equal(card.events.length, 0)
  assert.equal(card.effects[1].destroyed, false)
  await card.dismiss()
  card.effects[1].finishSurface()
  await pending
  assert.equal(card.phase.value, 'hidden')
  assert.deepEqual(card.events, ['dismiss'])
  assert.ok(card.effects[1].destroyed)
})

test('sudut memakai surface kartu dengan overlay di host luar, tanpa penundaan slide', async (t) => {
  const card = notice()
  t.after(card.stop)
  assert.equal(card.timers[1].delay, 0)
  await card.enter()
  assert.equal(card.effects[0].target, card.host)
  assert.equal(card.effects[0].options.surfaces, '[data-notification-card]')
  assert.equal(card.effects[0].within, card.surface.value)
})

test('timer tiga detik menyimpan sisa waktu saat hover atau fokus lalu memakai unreveal', async (t) => {
  const card = notice()
  t.after(card.stop)
  await card.enter()
  assert.equal(card.remaining.value, 3000)
  card.setTime(2000)
  card.props.paused = true
  await nextTick()
  assert.equal(card.remaining.value, 2000)
  assert.equal(card.timers[0].running, false)
  card.setTime(9000)
  card.props.paused = false
  await nextTick()
  assert.equal(card.remaining.value, 2000)
  assert.equal(card.timers[0].running, true)
  card.timers[0].callback()
  assert.equal(card.phase.value, 'unrevealing')
  assert.equal(card.events.length, 0)
  card.effects[1].finishHide()
  await flush()
  assert.equal(card.events.length, 0)
  card.effects[1].finishSurface()
  await flush()
  assert.deepEqual(card.events, ['dismiss'])
})

test('menutup saat reveal berjalan tidak merusak controller unreveal yang baru', async (t) => {
  const card = notice({ holdEntrance: true })
  t.after(card.stop)
  const entrance = card.enter()
  await flush()
  const closing = card.dismiss()
  assert.ok(card.effects[0].destroyed)
  card.effects[0].finishReveal()
  await entrance
  assert.equal(card.phase.value, 'unrevealing')
  assert.equal(card.effects[1].destroyed, false)
  card.effects[1].finishHide()
  await flush()
  card.effects[1].finishSurface()
  await closing
  assert.deepEqual(card.events, ['dismiss'])
})

test('reduce motion, tutup sebelum reveal, dan kegagalan efek tetap bisa menutup kartu', async () => {
  const reduced = notice({ motion: 'reduce' })
  await reduced.dismiss()
  assert.equal(reduced.effects.length, 0)
  assert.equal(reduced.phase.value, 'idle')
  assert.deepEqual(reduced.events, ['dismiss'])
  reduced.stop()

  const early = notice()
  await early.dismiss()
  assert.equal(early.effects.length, 0)
  assert.equal(early.timers[1].running, false)
  early.stop()

  const failed = notice({ failHide: true })
  await failed.enter()
  await failed.dismiss()
  assert.deepEqual(failed.events, ['dismiss'])
  failed.stop()
})

test('unmount membatalkan timer dan tidak mengirim dismiss setelah komponen hilang', async () => {
  const card = notice()
  await card.enter()
  const pending = card.dismiss()
  card.stop()
  assert.ok(card.timers.every(timer => !timer.running))
  card.effects[1].finishHide()
  await pending
  assert.equal(card.events.length, 0)
  assert.equal(card.effects[1].hides.length, 1)
})

test('unmount saat sudut berjalan juga menghentikan controller tanpa dismiss terlambat', async () => {
  const card = notice()
  await card.enter()
  const pending = card.dismiss()
  card.effects[1].finishHide()
  await flush()
  assert.equal(card.phase.value, 'unrevealing-surface')
  card.stop()
  assert.ok(card.effects[1].destroyed)
  card.effects[1].finishSurface()
  await pending
  assert.equal(card.events.length, 0)
})
