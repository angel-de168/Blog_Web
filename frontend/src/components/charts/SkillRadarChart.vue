<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RadarChart } from 'echarts/charts'
import {
  RadarComponent,
  TooltipComponent,
  type RadarComponentOption,
  type TooltipComponentOption
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { use, init, type ECharts, type ComposeOption } from 'echarts/core'
import type { RadarSeriesOption } from 'echarts/charts'
import type { SkillPoint } from '../../types'

use([RadarChart, RadarComponent, TooltipComponent, CanvasRenderer])

type ChartOption = ComposeOption<RadarComponentOption | TooltipComponentOption | RadarSeriesOption>

const props = defineProps<{
  points: SkillPoint[]
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const hasData = computed(() => props.points.length > 0)

async function renderChart() {
  if (!hasData.value) {
    return
  }

  await nextTick()
  if (!chartRef.value) {
    return
  }

  if (!chart) {
    chart = init(chartRef.value)
  }

  chart.resize()

  const option: ChartOption = {
    backgroundColor: 'transparent',
    tooltip: {
      backgroundColor: 'rgba(255, 247, 237, 0.96)',
      borderColor: '#ddc3a0',
      textStyle: { color: '#3a2b1f' }
    },
    radar: {
      radius: '60%',
      indicator: props.points.map((item) => ({ name: item.tag, max: 20 })),
      axisName: { color: '#5f4834' },
      axisLine: { lineStyle: { color: 'rgba(191, 157, 121, 0.3)' } },
      splitLine: { lineStyle: { color: 'rgba(191, 157, 121, 0.25)' } },
      splitArea: { areaStyle: { color: ['rgba(255, 249, 241, 0.72)', 'rgba(255, 241, 220, 0.62)'] } }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.points.map((item) => item.value),
            areaStyle: { color: 'rgba(193, 90, 35, 0.2)' },
            lineStyle: { color: '#c15a23', width: 2 },
            itemStyle: { color: '#c15a23' }
          }
        ]
      }
    ]
  }

  chart.setOption(option)
}

function handleResize() {
  if (!chart) {
    return
  }
  chart.resize()
}

watch(
  () => props.points,
  () => {
    void renderChart()
  },
  { deep: true }
)

onMounted(() => {
  void renderChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<template>
  <div v-if="hasData" ref="chartRef" class="chart"></div>
  <p v-else class="sub">先积累几条标签吧。</p>
</template>
