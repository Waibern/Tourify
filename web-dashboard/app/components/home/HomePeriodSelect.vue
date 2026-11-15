<script setup lang="ts">
import { eachDayOfInterval } from 'date-fns'
import type { Period, Range } from '~/types'

const model = defineModel<Period>({ required: true })

const props = defineProps<{
  range: Range
}>()

const days = computed(() => eachDayOfInterval(props.range))

const periods = computed(() => {
  let values: Period[]
  if (days.value.length <= 8) {
    values = ['daily']
  } else if (days.value.length <= 31) {
    values = ['daily', 'weekly']
  } else {
    values = ['weekly', 'monthly']
  }

  // Nilai periode dipakai untuk hitung grafik; menu menampilkan label bahasa Indonesia.
  const labels: Record<Period, string> = {
    daily: 'Harian',
    weekly: 'Mingguan',
    monthly: 'Bulanan'
  }

  return values.map(value => ({ label: labels[value], value }))
})

// Pastikan periode yang dipilih memang tersedia di menu.
watch(periods, () => {
  const values = periods.value.map(period => period.value)
  if (!values.includes(model.value)) {
    model.value = values[0]!
  }
})
</script>

<template>
  <USelect
    v-model="model"
    :items="periods"
    variant="ghost"
    class="data-[state=open]:bg-elevated"
    :ui="{ value: 'capitalize', itemLabel: 'capitalize', trailingIcon: 'group-data-[state=open]:rotate-180 transition-transform duration-200' }"
  />
</template>
