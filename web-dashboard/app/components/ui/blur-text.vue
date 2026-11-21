<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ text: string }>()

// Indeks kata mengatur jeda tanpa memecah teks menjadi karakter.
const segments = computed(() => {
  let wordIndex = 0

  return props.text.split(/(\s+)/u).filter(Boolean).map((text) => {
    const isWhitespace = /^\s+$/u.test(text)
    const segment = {
      text,
      isWhitespace,
      wordIndex: isWhitespace ? -1 : wordIndex
    }

    if (!isWhitespace) {
      wordIndex += 1
    }

    return segment
  })
})
</script>

<template>
  <!-- Teks lengkap dibaca sekali, sedangkan salinan visual dianimasikan. -->
  <span>
    <span class="sr-only">{{ props.text }}</span>
    <span class="blur-text__visual" aria-hidden="true">
      <template
        v-for="(segment, segmentIndex) in segments"
        :key="segmentIndex"
      >
        <span v-if="segment.isWhitespace">{{ segment.text }}</span>
        <span
          v-else
          class="blur-text__word"
          :style="{ '--blur-text-delay': `${segment.wordIndex * 180}ms` }"
        >{{ segment.text }}</span>
      </template>
    </span>
  </span>
</template>

<style scoped>
.blur-text__word {
  display: inline-block;
  white-space: nowrap;
}

/* Tiap kata mendapat satu efek blur lalu muncul bergiliran. */
@media (prefers-reduced-motion: no-preference) {
  .blur-text__word {
    animation: blur-text-enter 700ms cubic-bezier(0.16, 1, 0.3, 1) both;
    animation-delay: var(--blur-text-delay);
  }
}

@keyframes blur-text-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
    filter: blur(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}
</style>
