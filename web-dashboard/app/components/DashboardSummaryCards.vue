<script setup lang="ts">
interface SummaryCard {
  label: string
  value: string
  hint: string
  icon: string
  image?: string
}

const props = defineProps<{
  cards: SummaryCard[]
  loading: boolean
}>()

// Reveal awal cukup sekali; filter dan refresh tidak perlu memutar ulang skeleton.
const initialLoading = ref(props.loading)
watch(() => props.loading, (loading) => {
  if (!loading) initialLoading.value = false
})
</script>

<template>
  <!-- Ringkasan pakai susunan utama-kecil yang sama dengan kartu Overview. -->
  <section
    class="grid gap-3 min-[900px]:grid-cols-2"
    aria-label="Ringkasan data"
    :aria-busy="loading"
  >
    <article
      data-dashboard-card
      class="group block min-w-0 overflow-hidden rounded-xl border border-default bg-default transition-colors hover:border-white/35 hover:bg-elevated/40"
    >
      <AsciiReveal :loading="initialLoading" origin="pointer">
        <template #skeleton>
          <div class="h-44" aria-hidden="true" />
        </template>
        <div class="flex min-h-44 flex-col justify-between gap-3 p-4 sm:p-5">
          <div class="flex items-start justify-between gap-3">
            <h2 class="text-base font-semibold text-highlighted">
              {{ cards[0]?.label }}
            </h2>
            <UIcon
              :name="cards[0]?.icon"
              class="size-5 shrink-0 text-primary"
              aria-hidden="true"
            />
          </div>
          <div>
            <p class="text-4xl font-semibold tracking-tight text-highlighted tabular-nums">
              {{ cards[0]?.value }}
            </p>
            <p class="mt-1 text-sm text-muted">
              {{ cards[0]?.hint }}
            </p>
          </div>
        </div>
      </AsciiReveal>
    </article>

    <div class="grid min-w-0 gap-2">
      <article
        v-for="card in cards.slice(1)"
        :key="card.label"
        data-dashboard-card
        class="group relative flex min-w-0 items-center overflow-hidden rounded-xl border border-default bg-default transition-colors hover:border-white/35 hover:bg-elevated/40"
        :class="card.image ? 'min-h-24 p-0' : 'px-4 py-3'"
      >
        <AsciiReveal :loading="initialLoading" origin="pointer" class="relative min-w-0 flex-1">
          <template #skeleton>
            <div class="h-10" aria-hidden="true" />
          </template>
          <div
            :class="card.image
              ? 'relative flex min-h-24 w-full items-end justify-between gap-3 p-4'
              : 'flex min-w-0 flex-wrap items-center justify-between gap-x-3 gap-y-2'"
          >
            <img
              v-if="card.image"
              :src="card.image"
              alt=""
              aria-hidden="true"
              class="absolute inset-0 h-full w-full object-cover"
            >
            <div
              v-if="card.image"
              aria-hidden="true"
              class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/90 via-black/35 to-transparent"
            />
            <div :class="card.image ? 'relative z-10 min-w-0' : 'min-w-0'">
              <h2 :class="card.image ? 'text-sm font-medium text-white drop-shadow-sm' : 'text-sm font-medium text-highlighted'">
                {{ card.label }}
              </h2>
              <p :class="card.image ? 'text-sm text-white/85 drop-shadow-sm' : 'text-sm text-muted'">
                {{ card.hint }}
              </p>
            </div>
            <div
              :class="card.image
                ? 'relative z-10 flex max-w-[55%] items-center gap-2'
                : 'flex w-full shrink-0 items-center justify-end gap-2 lg:w-auto'"
            >
              <p
                :class="card.image
                  ? 'min-w-0 break-words text-right text-xl font-semibold text-white drop-shadow-sm tabular-nums'
                  : 'whitespace-nowrap text-right text-xl font-semibold text-highlighted tabular-nums'"
              >
                {{ card.value }}
              </p>
              <UIcon
                :name="card.icon"
                :class="card.image
                  ? 'size-4 shrink-0 text-white/85'
                  : 'size-4 text-muted transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5'"
                aria-hidden="true"
              />
            </div>
          </div>
        </AsciiReveal>
      </article>
    </div>
  </section>
</template>
