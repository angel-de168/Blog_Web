<script setup lang="ts">
import { computed } from 'vue'
import type { StatusPoint } from '../../types'

const props = defineProps<{
  points: StatusPoint[]
}>()

const total = computed(() => props.points.reduce((sum, item) => sum + item.count, 0))

function ratio(count: number) {
  if (!total.value) {
    return 0
  }
  return Math.round((count / total.value) * 100)
}

function statusText(status: StatusPoint['status']) {
  return status === 'MASTERED' ? '已掌握' : '学习中'
}
</script>

<template>
  <div class="status-stacks">
    <article v-for="item in points" :key="item.status" class="status-stack-item" :class="`status-${item.status.toLowerCase()}`">
      <p class="status-stack-name">{{ statusText(item.status) }}</p>
      <p class="status-stack-value">{{ item.count }} 篇</p>
      <p class="status-stack-ratio">{{ ratio(item.count) }}%</p>
    </article>
  </div>
</template>
