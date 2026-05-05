<script setup lang="ts">
import { computed } from 'vue'
import type { WeekdayPoint } from '../../types'

const props = defineProps<{
  points: WeekdayPoint[]
}>()

const maxCount = computed(() => Math.max(1, ...props.points.map((item) => item.count)))

function barHeight(count: number) {
  return Math.max(10, Math.round((count / maxCount.value) * 100))
}
</script>

<template>
  <div class="weekday-bars">
    <article v-for="item in points" :key="item.label" class="weekday-bar-item">
      <div class="weekday-bar-wrap">
        <span class="weekday-bar" :style="{ height: `${barHeight(item.count)}%` }"></span>
      </div>
      <span class="weekday-bar-count">{{ item.count }}</span>
      <span class="weekday-bar-label">{{ item.label }}</span>
    </article>
  </div>
</template>
