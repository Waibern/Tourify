<template>
  <!-- Latar gelap polos membingkai seluruh halaman login. -->
  <main class="flex min-h-dvh flex-col overflow-x-hidden bg-[#0b0b0d] text-white lg:grid lg:h-dvh lg:min-h-dvh lg:grid-cols-2">
    <section class="order-last flex flex-1 items-center justify-center px-6 py-10 sm:px-10 lg:order-first lg:min-h-dvh lg:px-12 xl:px-20">
      <!-- Form dibuat sedikit lebih ramping di dalam kolom. -->
      <div class="mx-auto w-full max-w-[28rem]">
        <slot />
      </div>
    </section>

    <!-- Frame desktop penuh tinggi, menempel kanan, dan digeser ke kiri. -->
    <aside class="relative order-first mx-3 my-3 h-[38svh] min-h-64 max-h-[24rem] overflow-hidden rounded-[1.5rem] lg:order-last lg:ml-8 lg:mr-0 lg:my-0 lg:h-dvh lg:min-h-dvh lg:max-h-none lg:rounded-l-[2rem] lg:rounded-r-none">
      <!-- Crop foto digeser lebih ke kiri supaya penyu tidak terlalu ke kanan. -->
      <img
        src="/wahana/samudra_biru.jpg"
        alt=""
        aria-hidden="true"
        class="absolute inset-0 size-full object-cover object-[65%_center]"
      >
      <div class="absolute inset-0 bg-gradient-to-br from-black/40 via-black/15 to-black/55" />

      <!-- Logo dan judul di atas; subjudul menjadi footer desktop. -->
      <div class="relative z-10 flex h-full flex-col px-6 py-6 sm:px-8 sm:py-8 lg:px-12 lg:py-12 xl:px-16 xl:py-14">
        <div data-login-copy="brand" class="flex items-center gap-4">
          <img
            src="/logo-putih.svg"
            alt=""
            aria-hidden="true"
            class="h-10 w-auto"
          >
          <span class="text-3xl font-semibold tracking-tight text-white drop-shadow-[0_2px_6px_rgba(0,0,0,0.55)]">Tourify</span>
        </div>

        <div class="mt-[4vh] max-w-xl max-[380px]:mt-[3vh] lg:mt-[10vh] lg:flex lg:flex-1 lg:flex-col">
          <h2 data-login-copy="title" class="max-w-[16ch] text-4xl font-semibold leading-[1.06] tracking-[-0.03em] text-white drop-shadow-[0_2px_6px_rgba(0,0,0,0.55)] max-[380px]:text-3xl sm:text-5xl xl:text-6xl">
            Kelola tiket dan kunjungan dalam satu tempat.
          </h2>
          <p data-login-copy="description" class="mt-5 text-base text-white/85 drop-shadow-[0_2px_6px_rgba(0,0,0,0.55)] sm:text-lg lg:mt-auto">
            Dashboard admin &amp; gate
          </p>
        </div>
      </div>
    </aside>
  </main>
</template>

<style scoped>
@media (prefers-reduced-motion: no-preference) {
  [data-login-copy] {
    animation: login-copy-reveal 650ms cubic-bezier(0.16, 1, 0.3, 1) both;
  }

  [data-login-copy='brand'] {
    animation-delay: 80ms;
  }

  [data-login-copy='title'] {
    animation-delay: 220ms;
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
