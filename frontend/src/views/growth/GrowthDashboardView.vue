<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { fetchGrowthOverview } from '../../api/growth'
import type { GrowthOverview } from '../../types'

const TimelineChart = defineAsyncComponent(() => import('../../components/charts/TimelineChart.vue'))
const SkillRadarChart = defineAsyncComponent(() => import('../../components/charts/SkillRadarChart.vue'))
const WeekdayBarsChart = defineAsyncComponent(() => import('../../components/charts/WeekdayBarsChart.vue'))
const StatusStackChart = defineAsyncComponent(() => import('../../components/charts/StatusStackChart.vue'))
const LoopTimelineChart = defineAsyncComponent(() => import('../../components/charts/LoopTimelineChart.vue'))

const days = ref(120)
const loading = ref(false)
const error = ref('')
const overview = ref<GrowthOverview>({
  timeline: [],
  skills: [],
  weekday: [],
  statuses: [],
  summary: { totalNotes: 0, activeDays: 0, peakDayCount: 0, avgPerWeek: 0 },
  loopSummary: {
    notesCreated: 0,
    momentsCreated: 0,
    totalOutput: 0,
    noteCommentsReceived: 0,
    momentCommentsReceived: 0,
    momentLikesReceived: 0,
    totalFeedback: 0,
    notesIterated: 0,
    iterationCoverage: 0,
    avgIterationLagDays: 0,
    feedbackPerOutput: 0
  },
  loopTimeline: []
})
const heroImage = 'https://picsum.photos/id/1043/2200/1200'
const hasOverviewData = computed(
  () =>
    overview.value.timeline.length > 0 ||
    overview.value.skills.length > 0 ||
    overview.value.weekday.length > 0 ||
    overview.value.statuses.length > 0 ||
    overview.value.loopTimeline.length > 0 ||
    overview.value.loopSummary.totalOutput > 0 ||
    overview.value.loopSummary.totalFeedback > 0 ||
    overview.value.loopSummary.notesIterated > 0
)

const creationYearBlocks = computed(() => {
  const yearCountMap = new Map<string, number>()
  for (const item of overview.value.timeline) {
    const year = item.date.slice(0, 4)
    if (!year) {
      continue
    }
    yearCountMap.set(year, (yearCountMap.get(year) ?? 0) + item.count)
  }
  return Array.from(yearCountMap.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([year, count]) => ({ year, count }))
    .slice(-4)
}
)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    overview.value = await fetchGrowthOverview(days.value)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-scene growth-scene">
    <header class="page-hero" :style="{ '--page-hero-image': `url(${heroImage})` }">
      <div class="page-hero-copy">
        <p class="page-hero-kicker">GROWTH MAP</p>
        <h1>成长轨迹</h1>
        <p class="sub">看趋势，比卷时长更重要。</p>
      </div>
    </header>

    <section class="page-grid growth-grid">
      <article class="card wide growth-main">
        <div class="row page-head growth-head">
          <div>
            <h1>成长轨迹</h1>
            <p class="sub">看趋势，比卷时长更重要。</p>
          </div>
          <select v-model.number="days" @change="loadData">
            <option :value="30">近30天</option>
            <option :value="90">近90天</option>
            <option :value="120">近120天</option>
          </select>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-else-if="loading" class="sub">图表加载中...</p>
        <p v-else-if="!hasOverviewData" class="sub">暂无主用户成长数据。</p>

        <template v-else>
          <div class="growth-summary-grid">
            <article class="growth-summary-item">
              <p class="growth-summary-label">总产出</p>
              <p class="growth-summary-value">{{ overview.summary.totalNotes }}</p>
            </article>
            <article class="growth-summary-item">
              <p class="growth-summary-label">活跃天数</p>
              <p class="growth-summary-value">{{ overview.summary.activeDays }}</p>
            </article>
            <article class="growth-summary-item">
              <p class="growth-summary-label">单日峰值</p>
              <p class="growth-summary-value">{{ overview.summary.peakDayCount }}</p>
            </article>
            <article class="growth-summary-item">
              <p class="growth-summary-label">周均产出</p>
              <p class="growth-summary-value">{{ overview.summary.avgPerWeek }}</p>
            </article>
          </div>

          <div class="growth-chart-wrap">
            <TimelineChart :points="overview.timeline" />
          </div>

          <section class="growth-loop-grid growth-main-loop-grid">
            <section class="growth-extra-card growth-loop-timeline-card">
              <h3>闭环时间线（产出/反馈/迭代）</h3>
              <div class="growth-loop-chart-wrap">
                <LoopTimelineChart :points="overview.loopTimeline" />
              </div>
            </section>
          </section>

        </template>
      </article>

      <article class="card growth-side">
        <h2>技能雷达</h2>
        <div class="growth-radar-wrap">
          <SkillRadarChart :points="overview.skills" />
        </div>

        <div class="growth-side-stack">
          <section class="growth-extra-card">
            <h3>周内活跃分布</h3>
            <WeekdayBarsChart :points="overview.weekday" />
          </section>

          <section class="growth-loop-grid">
            <div class="growth-loop-kpis growth-loop-kpis-compact">
              <article class="growth-loop-kpi-item growth-loop-kpi-status">
                <h3>状态结构</h3>
                <StatusStackChart :points="overview.statuses" />
              </article>
              <article class="growth-loop-kpi-item">
                <p class="growth-loop-kpi-label">个人成就</p>
                <p class="growth-loop-kpi-sub growth-achievement-item is-like">
                  <svg class="growth-achievement-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 21s-7-4.3-9.3-8A5.5 5.5 0 0 1 12 6.4 5.5 5.5 0 0 1 21.3 13c-2.3 3.7-9.3 8-9.3 8Z" />
                  </svg>
                  <span>博客获得 0 次点赞</span>
                </p>
                <p class="growth-loop-kpi-sub growth-achievement-item is-comment">
                  <svg class="growth-achievement-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 5h16v10H8l-4 4V5Z" />
                  </svg>
                  <span>博客获得 {{ overview.loopSummary.noteCommentsReceived }} 次评论</span>
                </p>
                <p class="growth-loop-kpi-sub growth-achievement-item is-like growth-achievement-break">
                  <svg class="growth-achievement-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 21s-7-4.3-9.3-8A5.5 5.5 0 0 1 12 6.4 5.5 5.5 0 0 1 21.3 13c-2.3 3.7-9.3 8-9.3 8Z" />
                  </svg>
                  <span>随笔获得 {{ overview.loopSummary.momentLikesReceived }} 次点赞</span>
                </p>
                <p class="growth-loop-kpi-sub growth-achievement-item is-comment">
                  <svg class="growth-achievement-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 5h16v10H8l-4 4V5Z" />
                  </svg>
                  <span>随笔获得 {{ overview.loopSummary.momentCommentsReceived }} 次评论</span>
                </p>
              </article>
              <article class="growth-loop-kpi-item">
                <p class="growth-loop-kpi-label">内容总量</p>
                <p class="growth-loop-kpi-sub">博客 {{ overview.loopSummary.notesCreated }} 篇</p>
                <p class="growth-loop-kpi-sub">随笔 {{ overview.loopSummary.momentsCreated }} 篇</p>
              </article>
              <article class="growth-loop-kpi-item">
                <p class="growth-loop-kpi-label">创作历程</p>
                <div v-if="creationYearBlocks.length" class="growth-year-grid">
                  <div v-for="item in creationYearBlocks" :key="item.year" class="growth-year-item">
                    <p class="growth-year-count">{{ item.count }}篇</p>
                    <p class="growth-year-label">{{ item.year }}</p>
                  </div>
                </div>
                <p v-else class="growth-loop-kpi-sub">当前区间暂无博客发布</p>
              </article>
            </div>
          </section>
        </div>

        <hr class="split-line" />
        <p class="sub">保持频率，波动会收敛。</p>
        <p class="sub">今天写的，会在下周变稳。</p>
      </article>
    </section>
  </section>
</template>
