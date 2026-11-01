<template>
  <UDashboardPanel id="profile" :ui="{ body: 'lg:py-8' }">
    <template #header>
      <UDashboardNavbar title="Profil">
        <template #leading>
          <UDashboardSidebarCollapse />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <!-- Form profil memakai lebar konten dashboard seperti contoh yang dikirim. -->
      <div class="flex w-full flex-col gap-4 sm:gap-6 lg:gap-8">
        <NuxtPage />
      </div>
    </template>
  </UDashboardPanel>
</template>
