<template>
  <!-- Latar gelap menjaga kontras form dan screenshot dashboard. -->
  <main class="flex min-h-dvh flex-col overflow-x-hidden bg-[#0e0e0e] text-white lg:grid lg:h-dvh lg:min-h-dvh lg:grid-cols-[0.9fr_1.1fr]">
    <section class="order-last flex flex-1 items-center justify-center px-6 py-10 sm:px-10 lg:order-first lg:min-h-dvh lg:px-12 xl:px-20">
      <!-- Lebar form dijaga supaya tetap nyaman dibaca. -->
      <div class="mx-auto w-full max-w-[28rem]">
        <slot />
      </div>
    </section>

    <!-- Screenshot utuh memakai rasio asli supaya seluruh dashboard terlihat. -->
    <aside class="order-first mx-3 mt-3 flex aspect-[2992/1722] max-h-[42svh] items-center justify-center overflow-hidden rounded-[1.5rem] bg-[#151515] p-2 lg:order-last lg:mx-0 lg:my-0 lg:h-dvh lg:max-h-none lg:rounded-none lg:p-8">
      <img
        src="/tourify-dashboard-overview.png"
        alt="Screenshot penuh halaman Overview dashboard Tourify"
        class="block max-h-full max-w-full rounded-xl object-contain"
      >
    </aside>
  </main>
</template>
