<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  type GridComponentOption,
  type TooltipComponentOption
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { use, init, type ECharts, type ComposeOption } from 'echarts/core'
import type { LineSeriesOption } from 'echarts/charts'
import type { TimelinePoint } from '../../types'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

type ChartOption = ComposeOption<GridComponentOption | TooltipComponentOption | LineSeriesOption>

const props = defineProps<{
  points: TimelinePoint[]
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const hasData = computed(() => props.points.length > 0)

function renderChart() {
  if (!chartRef.value || !hasData.value) {
    return
  }

  if (!chart) {
    chart = init(chartRef.value)
  }

  const option: ChartOption = {
    backgroundColor: 'transparent',
    grid: { left: 28, right: 18, top: 28, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 247, 237, 0.96)',
      borderColor: '#ddc3a0',
      textStyle: { color: '#3a2b1f' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.points.map((item) => item.date),
      axisLine: { lineStyle: { color: '#ccb390' } },
      axisLabel: { color: '#705640' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#ccb390' } },
      splitLine: { lineStyle: { color: 'rgba(191, 157, 121, 0.24)' } },
      axisLabel: { color: '#705640' }
    },
    series: [
      {
        type: 'line',
        data: props.points.map((item) => item.count),
        smooth: true,
        lineStyle: { color: '#c15a23', width: 3 },
        areaStyle: { color: 'rgba(193, 90, 35, 0.18)' },
        itemStyle: { color: '#c15a23' }
      }
    ]
  }

  chart.setOption(option)
}

watch(
  () => props.points,
  () => {
    renderChart()
  },
  { deep: true }
)

onMounted(renderChart)
onBeforeUnmount(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<template>
  <div v-if="hasData" ref="chartRef" class="chart"></div>
  <p v-else class="sub">这个区间还没数据。</p>
</template>
