<script setup lang="ts">
import type { DropdownMenuItem } from '@nuxt/ui'
import type { DashboardMember, DashboardRole } from '~/types/tourify'

const props = defineProps<{
  members: DashboardMember[]
  updatingMemberId?: number | null
}>()

const emit = defineEmits<{
  changeRole: [member: DashboardMember, role: DashboardRole]
  removeMember: [member: DashboardMember]
}>()

const auth = useDashboardAuth()
const api = useTourifyApi()
const roleOptions = [
  { label: 'Admin', value: 'admin' },
  { label: 'Petugas', value: 'gate' }
]

function initials(username: string): string {
  return username.trim().slice(0, 2).toLocaleUpperCase('id-ID') || '?'
}

function updateRole(member: DashboardMember, value: string | number) {
  if (value === 'admin' || value === 'gate') emit('changeRole', member, value)
}

// Menu aksi dipakai untuk menghapus akun anggota lain.
function actions(member: DashboardMember): DropdownMenuItem[] {
  return [{
    label: 'Hapus anggota',
    color: 'error',
    onSelect: () => emit('removeMember', member)
  }]
}
</script>

<template>
  <ul role="list" class="divide-y divide-default">
    <li
      v-for="member in props.members"
      :key="member.id"
      class="flex flex-col gap-3 py-3 px-4 sm:flex-row sm:items-center sm:justify-between sm:px-6"
    >
      <div class="flex items-center gap-3 min-w-0">
        <UAvatar
          :src="api.assetUrl(member.profile_image)"
          :alt="'Foto profil ' + member.username"
          :text="initials(member.username)"
          size="md"
        />
        <div class="text-sm min-w-0">
          <p class="text-highlighted font-medium truncate">
            {{ member.username }}
          </p>
          <p class="text-muted truncate">
            ID {{ member.id }}
          </p>
        </div>
      </div>

      <!-- Kontrol turun ke baris kedua di ponsel supaya username tetap terbaca. -->
      <div class="flex items-center gap-3 self-end sm:self-auto">
        <USelect
          :model-value="member.role"
          :items="roleOptions"
          color="neutral"
          class="min-w-28"
          :ui="{ value: 'capitalize', item: 'capitalize' }"
          :disabled="member.id === auth.user.value?.id || member.id === props.updatingMemberId"
          :aria-label="`Peran ${member.username}`"
          @update:model-value="updateRole(member, $event)"
        />

        <UDropdownMenu
          v-if="member.id !== auth.user.value?.id"
          :items="actions(member)"
          :content="{ align: 'end' }"
        >
          <UButton
            icon="i-lucide-ellipsis-vertical"
            color="neutral"
            variant="ghost"
            :aria-label="`Aksi untuk ${member.username}`"
          />
        </UDropdownMenu>
      </div>
    </li>
  </ul>
</template>
