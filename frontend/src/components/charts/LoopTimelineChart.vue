<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  type GridComponentOption,
  type LegendComponentOption,
  type TooltipComponentOption
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { use, init, type ECharts, type ComposeOption } from 'echarts/core'
import type { LineSeriesOption } from 'echarts/charts'
import type { LoopTimelinePoint } from '../../types'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type ChartOption = ComposeOption<
  GridComponentOption | LegendComponentOption | TooltipComponentOption | LineSeriesOption
>

const props = defineProps<{
  points: LoopTimelinePoint[]
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

const hasData = computed(() => props.points.length > 0)

const xLabelStep = computed(() => {
  const count = props.points.length
  if (count <= 14) {
    return 1
  }
  if (count <= 31) {
    return 3
  }
  if (count <= 62) {
    return 6
  }
  return 10
})

function shouldShowLabel(index: number) {
  const lastIndex = props.points.length - 1
  if (index === 0 || index === lastIndex) {
    return true
  }
  return index % xLabelStep.value === 0
}

function shortDateLabel(date: string) {
  const [year, month, day] = date.split('-')
  if (!year || !month || !day) {
    return date
  }
  return `${month}-${day}`
}

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
    grid: { left: 32, right: 18, top: 44, bottom: 24 },
    legend: {
      top: 10,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#5f4834' },
      data: ['产出', '反馈', '迭代']
    },
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
      axisTick: { alignWithLabel: true },
      axisLabel: {
        color: '#705640',
        interval: 0,
        formatter: (value: string, index: number) =>
          shouldShowLabel(index) ? shortDateLabel(value) : ''
      }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#ccb390' } },
      splitLine: { lineStyle: { color: 'rgba(191, 157, 121, 0.24)' } },
      axisLabel: { color: '#705640' }
    },
    series: [
      {
        name: '产出',
        type: 'line',
        smooth: true,
        data: props.points.map((item) => item.outputCount),
        lineStyle: { color: '#3d9585', width: 2.5 },
        itemStyle: { color: '#3d9585' }
      },
      {
        name: '反馈',
        type: 'line',
        smooth: true,
        data: props.points.map((item) => item.feedbackCount),
        lineStyle: { color: '#56a999', width: 2.5 },
        itemStyle: { color: '#56a999' }
      },
      {
        name: '迭代',
        type: 'line',
        smooth: true,
        data: props.points.map((item) => item.iterationCount),
        lineStyle: { color: '#df985c', width: 2.5 },
        itemStyle: { color: '#df985c' }
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
  <p v-else class="sub">暂无闭环时间线数据。</p>
</template>
