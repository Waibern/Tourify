<script setup lang="ts">
import type { DashboardActivityNotification } from '~/composables/useDashboardActivity'

const { isNotificationsSlideoverOpen } = useDashboard()
const notifications = useDashboardActivityFeed()

function activityPath(notification: DashboardActivityNotification): string {
  const activity = notification.activity
  if (activity.type === 'order') {
    return `/orders?ticket=${encodeURIComponent(activity.entity_id)}`
  }

  return `/customers?customer=${encodeURIComponent(activity.entity_id)}`
}

function activityTitle(notification: DashboardActivityNotification): string {
  const activity = notification.activity
  return activity.type === 'order' ? `Pesanan baru #${activity.entity_id}` : 'Pelanggan baru'
}

function activityBody(notification: DashboardActivityNotification): string {
  const activity = notification.activity
  if (activity.type === 'order') {
    return `${activity.username} memesan ${activity.quantity} tiket di ${activity.wahana_name}.`
  }

  return `${activity.username} baru terdaftar di Tourify.`
}

function formatTimeAgo(value: string): string {
  const elapsed = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000))
  if (!Number.isFinite(elapsed) || elapsed < 60) return 'Baru saja'
  if (elapsed < 3600) return `${Math.floor(elapsed / 60)} menit lalu`
  if (elapsed < 86400) return `${Math.floor(elapsed / 3600)} jam lalu`
  return `${Math.floor(elapsed / 86400)} hari lalu`
}

function markAsRead(notification: DashboardActivityNotification) {
  notification.unread = false
  isNotificationsSlideoverOpen.value = false
}
</script>

<template>
  <USlideover
    v-model:open="isNotificationsSlideoverOpen"
    title="Notifikasi"
  >
    <template #body>
      <div v-if="!notifications.length" role="status" class="grid min-h-40 place-items-center text-center">
        <div>
          <UIcon name="i-lucide-bell" class="mb-2 size-6 text-muted" />
          <p class="text-sm text-muted">
            Notifikasi pesanan dan pelanggan baru akan muncul di sini.
          </p>
        </div>
      </div>

      <NuxtLink
        v-for="notification in notifications"
        :key="notification.activity.id"
        :to="activityPath(notification)"
        class="relative -mx-3 flex items-center gap-3 rounded-md px-3 py-2.5 hover:bg-elevated/50 first:-mt-3 last:-mb-3"
        @click="markAsRead(notification)"
      >
        <UChip
          color="error"
          :show="notification.unread"
          inset
        >
          <UAvatar
            :text="notification.activity.username.slice(0, 1).toUpperCase()"
            :alt="notification.activity.username"
            size="md"
          />
        </UChip>

        <div class="min-w-0 flex-1 text-sm">
          <p class="flex items-center justify-between gap-3">
            <span class="truncate font-medium text-highlighted">{{ activityTitle(notification) }}</span>
            <time
              :datetime="notification.activity.occurred_at"
              class="shrink-0 text-xs text-muted"
            >
              {{ formatTimeAgo(notification.activity.occurred_at) }}
            </time>
          </p>
          <p class="text-dimmed">
            {{ activityBody(notification) }}
          </p>
        </div>
      </NuxtLink>
    </template>
  </USlideover>
</template>
