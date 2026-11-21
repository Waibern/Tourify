<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import BlurText from '~/components/ui/blur-text.vue'

// Foto wahana untuk panel login.
const heroPhotos = [
  { src: '/wahana/samudra_biru.jpg', name: 'Samudra Biru' },
  { src: '/wahana/pantai_mentari.jpg', name: 'Pantai Mentari' },
  { src: '/wahana/pulau_air.jpg', name: 'Pulau Air' },
  { src: '/wahana/taman_satwa_pesisir.jpg', name: 'Taman Satwa Pesisir' },
  { src: '/wahana/dunia_petualang.jpg', name: 'Dunia Petualang' }
]

const currentPhotoIndex = ref(0)
// Setelah foto terakhir, mulai lagi dari foto pertama.
const activePhoto = computed(() => heroPhotos[currentPhotoIndex.value]!)
let photoInterval: ReturnType<typeof setInterval>

// Ganti foto tiap tujuh detik, lalu hentikan timer saat layout ditutup.
onMounted(() => {
  photoInterval = setInterval(() => {
    currentPhotoIndex.value = (currentPhotoIndex.value + 1) % heroPhotos.length
  }, 7000)
})

onUnmounted(() => {
  clearInterval(photoInterval)
})
</script>

<template>
  <!-- Latar gelap membingkai halaman login. -->
  <main class="flex min-h-dvh flex-col overflow-x-hidden bg-[#0e0e0e] text-white lg:grid lg:h-dvh lg:min-h-dvh lg:grid-cols-2">
    <section class="order-last flex flex-1 items-center justify-center px-6 py-10 sm:px-10 lg:order-first lg:min-h-dvh lg:px-12 xl:px-20">
      <!-- Lebar form dijaga supaya tetap nyaman dibaca. -->
      <div class="mx-auto w-full max-w-[28rem]">
        <slot />
      </div>
    </section>

    <!-- Foto membulat di sisi kiri dan tetap rata di sisi kanan. -->
    <aside class="relative order-first mx-3 my-3 h-[38svh] min-h-64 max-h-[24rem] overflow-hidden rounded-[1.5rem] lg:order-last lg:ml-8 lg:mr-0 lg:my-0 lg:h-dvh lg:min-h-dvh lg:max-h-none lg:rounded-l-[3rem] lg:rounded-r-none">
      <!-- Ganti foto tanpa menggeser isi panel. -->
      <Transition name="login-photo">
        <img
          :key="activePhoto.src"
          :src="activePhoto.src"
          alt=""
          aria-hidden="true"
          class="absolute inset-0 size-full object-cover object-[65%_center]"
        >
      </Transition>
      <div class="absolute inset-0 bg-gradient-to-br from-black/40 via-black/15 to-black/55" />

      <!-- Logo, nama dashboard, dan judul tetap terlihat di atas foto. -->
      <div class="relative z-10 flex h-full flex-col px-6 py-6 sm:px-8 sm:py-8 lg:px-12 lg:py-12 xl:px-16 xl:py-14">
        <div data-login-copy="brand" class="flex items-center gap-3">
          <img
            src="/logo-putih.svg"
            alt=""
            aria-hidden="true"
            class="h-8 w-auto"
          >
          <div class="flex items-baseline gap-2">
            <span class="text-2xl font-semibold tracking-tight text-white drop-shadow-[0_2px_6px_rgba(0,0,0,0.55)]">Tourify</span>
            <span class="text-sm font-medium tracking-wide text-white/85 drop-shadow-[0_2px_6px_rgba(0,0,0,0.75)]">Dashboard</span>
          </div>
        </div>

        <div class="mt-[4vh] max-w-xl max-[380px]:mt-[3vh] lg:mt-[10vh] lg:flex lg:flex-1 lg:flex-col">
          <!-- Tagline muncul bertahap dengan efek blur. -->
          <h2 class="login-title-shadow max-w-[16ch] text-4xl font-semibold leading-[1.06] tracking-[-0.03em] text-white max-[380px]:text-3xl sm:text-5xl xl:text-6xl">
            <BlurText text="Kelola tiket dan kunjungan dalam satu tempat." />
          </h2>
          <div class="mt-5 lg:mt-auto">
            <!-- Pilih titik untuk langsung menampilkan foto yang diinginkan. -->
            <div class="mb-6 flex items-center gap-1" role="group" aria-label="Pilih foto wisata">
              <button
                v-for="(photo, index) in heroPhotos"
                :key="photo.src"
                type="button"
                class="flex size-10 items-center justify-center rounded-full focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
                :aria-label="`Tampilkan foto ${photo.name}`"
                :aria-pressed="currentPhotoIndex === index"
                @click="currentPhotoIndex = index"
              >
                <span
                  aria-hidden="true"
                  class="login-photo-dot size-2 shrink-0 rounded-full bg-white/40"
                  :class="currentPhotoIndex === index ? 'login-photo-dot--active bg-white' : ''"
                />
              </button>
            </div>
            <!-- Keterangan dibuat ringkas supaya foto tetap menonjol. -->
            <p data-login-copy="description" class="text-sm text-white/85 drop-shadow-[0_2px_6px_rgba(0,0,0,0.55)]">
              Dashboard admin &amp; gate
            </p>
          </div>
        </div>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.login-photo-enter-active,
.login-photo-leave-active {
  transition: opacity 700ms cubic-bezier(0.23, 1, 0.32, 1);
}

.login-photo-enter-from,
.login-photo-leave-to {
  opacity: 0;
}

.login-photo-dot {
  transform: scale(1);
  transition: transform 200ms cubic-bezier(0.23, 1, 0.32, 1), background-color 200ms ease;
}

.login-photo-dot--active {
  transform: scale(1.35);
}

/* Bayangan lembut menjaga judul tetap terbaca di atas foto. */
.login-title-shadow {
  text-shadow: 0 2px 6px rgb(0 0 0 / 0.55);
}

@media (prefers-reduced-motion: reduce) {
  .login-photo-enter-active,
  .login-photo-leave-active,
  .login-photo-dot {
    transition-duration: 120ms;
  }
}

@media (prefers-reduced-motion: no-preference) {
  [data-login-copy] {
    animation: login-copy-reveal 650ms cubic-bezier(0.16, 1, 0.3, 1) both;
  }

  [data-login-copy='brand'] {
    animation-delay: 80ms;
  }

  [data-login-copy='description'] {
    animation-delay: 360ms;
  }
}

@keyframes login-copy-reveal {
  from {
    opacity: 0;
    transform: translate3d(0, 14px, 0);
    filter: blur(3px);
  }

  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
    filter: blur(0);
  }
}
</style>
