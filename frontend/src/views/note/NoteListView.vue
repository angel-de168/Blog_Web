<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteNote, fetchNotes } from '../../api/note'
import { useAuthStore } from '../../stores/auth'
import type { NoteItem, NoteQuery } from '../../types'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const query = reactive({
  keyword: '',
  status: '' as '' | 'LEARNING' | 'MASTERED',
  contentKeyword: '',
  startTime: '',
  endTime: '',
  minReadCount: null as number | null,
  maxReadCount: null as number | null,
  sortBy: 'updatedAt' as 'updatedAt' | 'contentLength' | 'readCount',
  sortOrder: 'desc' as 'asc' | 'desc',
  page: 1,
  size: 10
})

const total = ref(0)
const loading = ref(false)
const notes = ref<NoteItem[]>([])
const error = ref('')
const success = ref('')
const createNoticeVisible = ref(false)
const filterModalVisible = ref(false)
const filterBasicExpanded = ref(true)
const filterSortExpanded = ref(false)
const heroImage = 'https://picsum.photos/id/1018/2200/1200'

const cardCovers = [
  'https://picsum.photos/id/1027/860/560',
  'https://picsum.photos/id/1050/860/560',
  'https://picsum.photos/id/1060/860/560',
  'https://picsum.photos/id/1035/860/560'
]

const learningCount = computed(() => notes.value.filter((item) => item.status === 'LEARNING').length)
const masteredCount = computed(() => notes.value.filter((item) => item.status === 'MASTERED').length)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const isPrevDisabled = computed(() => loading.value || query.page <= 1)
const isNextDisabled = computed(() => loading.value || query.page >= totalPages.value)

const trendDays = computed(() => {
  const now = new Date()
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(now)
    date.setDate(now.getDate() - (6 - index))
    return {
      key: date.toISOString().slice(0, 10),
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      count: 0
    }
  })

  const dayMap = new Map(days.map((day) => [day.key, day]))
  notes.value.forEach((item) => {
    const date = new Date(noteDate(item))
    if (Number.isNaN(date.getTime())) {
      return
    }
    const key = date.toISOString().slice(0, 10)
    const target = dayMap.get(key)
    if (target) {
      target.count += 1
    }
  })

  const max = Math.max(1, ...days.map((day) => day.count))
  return days.map((day) => ({
    ...day,
    height: Math.max(14, Math.round((day.count / max) * 100))
  }))
})

const latestSideNotes = computed(() =>
  [...notes.value]
    .sort((a, b) => new Date(noteDate(b)).getTime() - new Date(noteDate(a)).getTime())
    .slice(0, 3)
)

const topTags = computed(() => {
  const countMap = new Map<string, number>()
  notes.value.forEach((item) => {
    item.tags.forEach((tag) => {
      const key = tag.trim()
      if (!key) {
        return
      }
      countMap.set(key, (countMap.get(key) || 0) + 1)
    })
  })

  return [...countMap.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name, count }))
})

const hasActiveFilter = computed(
  () =>
    !!query.keyword.trim() ||
    !!query.contentKeyword.trim() ||
    !!query.status ||
    !!query.startTime.trim() ||
    !!query.endTime.trim() ||
    query.minReadCount !== null ||
    query.maxReadCount !== null ||
    query.sortBy !== 'updatedAt' ||
    query.sortOrder !== 'desc'
)

const activeFilterCount = computed(() => {
  let count = 0
  if (query.status) {
    count += 1
  }
  if (query.contentKeyword.trim()) {
    count += 1
  }
  if (query.startTime.trim()) {
    count += 1
  }
  if (query.endTime.trim()) {
    count += 1
  }
  if (query.minReadCount !== null) {
    count += 1
  }
  if (query.maxReadCount !== null) {
    count += 1
  }
  if (query.sortBy !== 'updatedAt' || query.sortOrder !== 'desc') {
    count += 1
  }
  return count
})

function normalizeReadCount(value: number | string | null) {
  if (value === null || value === '') {
    return null
  }
  const numeric = Number(value)
  if (Number.isNaN(numeric)) {
    return null
  }
  return Math.max(0, Math.floor(numeric))
}

function normalizeFilterValues() {
  query.keyword = query.keyword.trim()
  query.contentKeyword = query.contentKeyword.trim()
  query.startTime = query.startTime.trim()
  query.endTime = query.endTime.trim()
  query.minReadCount = normalizeReadCount(query.minReadCount)
  query.maxReadCount = normalizeReadCount(query.maxReadCount)
}

function buildQueryParams(): NoteQuery {
  normalizeFilterValues()
  return {
    keyword: query.keyword || undefined,
    status: query.status || undefined,
    contentKeyword: query.contentKeyword || undefined,
    startTime: query.startTime ? `${query.startTime}T00:00:00` : undefined,
    endTime: query.endTime ? `${query.endTime}T23:59:59` : undefined,
    minReadCount: query.minReadCount ?? undefined,
    maxReadCount: query.maxReadCount ?? undefined,
    sortBy: query.sortBy,
    sortOrder: query.sortOrder,
    page: query.page,
    size: query.size
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchNotes(buildQueryParams())
    notes.value = res.records
    total.value = res.total
    query.page = res.page
    query.size = res.size
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}


function applySuccessFromRoute() {
  const saved = route.query.saved
  if (saved === 'created') {
    success.value = '发布成功'
  } else if (saved === 'updated') {
    success.value = '保存成功'
  } else {
    return
  }
  void router.replace({ path: route.path, query: { ...route.query, saved: undefined } })
}

async function applyFilter() {
  query.page = 1
  await loadData()
}

function openFilterModal() {
  filterModalVisible.value = true
}

function toggleFilterBasic() {
  filterBasicExpanded.value = !filterBasicExpanded.value
}

function toggleFilterSort() {
  filterSortExpanded.value = !filterSortExpanded.value
}

function closeFilterModal() {
  filterModalVisible.value = false
}

async function applyFilterFromModal() {
  query.page = 1
  await loadData()
  closeFilterModal()
}

function resetFilters() {
  query.status = ''
  query.contentKeyword = ''
  query.startTime = ''
  query.endTime = ''
  query.minReadCount = null
  query.maxReadCount = null
  query.sortBy = 'updatedAt'
  query.sortOrder = 'desc'
}

async function clearAndReloadFilters() {
  resetFilters()
  query.page = 1
  await loadData()
  closeFilterModal()
}

async function goPrev() {
  if (isPrevDisabled.value) {
    return
  }
  query.page -= 1
  await loadData()
}

async function goNext() {
  if (isNextDisabled.value) {
    return
  }
  query.page += 1
  await loadData()
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

function statusText(status: NoteItem['status']) {
  return status === 'MASTERED' ? '已掌握' : '学习中'
}

function firstTag(item: NoteItem) {
  return item.tags[0] || '未分类'
}

function noteDate(item: NoteItem) {
  return item.updatedAt || item.createdAt
}

function notePreview(content: string) {
  const text = content.trim()
  if (text.length <= 64) {
    return text || '先记下这次思考。'
  }
  return `${text.slice(0, 64)}...`
}

function coverByIndex(index: number) {
  return cardCovers[index % cardCovers.length]
}

function goDetail(id: number) {
  void router.push(`/notes/${id}`)
}

async function remove(id: number) {
  if (!authStore.user) {
    error.value = '请登录后使用'
    return
  }
  await deleteNote(id)
  if (notes.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadData()
}

function closeCreateNotice() {
  createNoticeVisible.value = false
}

function goCreate() {
  if (authStore.user) {
    void router.push('/notes/new')
    return
  }
  createNoticeVisible.value = true
}

function goLoginForCreate() {
  createNoticeVisible.value = false
  void router.push({ name: 'login', query: { redirect: '/notes/new' } })
}

onMounted(async () => {
  applySuccessFromRoute()
  await loadData()
})
</script>

<template>
  <section class="page-scene notes-scene">
    <header class="page-hero" :style="{ '--page-hero-image': `url(${heroImage})` }">
      <div class="page-hero-copy">
        <p class="page-hero-kicker">BLOG NOTES</p>
        <h1>我的博客</h1>
        <p class="sub">先写，再打磨。</p>
      </div>
    </header>


    <div v-if="createNoticeVisible" class="login-notice-mask" @click="closeCreateNotice">
      <div class="login-notice" role="dialog" aria-modal="true" @click.stop>
        <p>请登录后使用</p>
        <div class="login-notice-actions">
          <button class="primary-btn" type="button" @click="goLoginForCreate">去登录</button>
          <button class="ghost-btn" type="button" @click="closeCreateNotice">知道了</button>
        </div>
      </div>
    </div>

    <section class="page-grid notes-grid">
      <aside class="notes-side">
        <article class="card notes-side-card notes-side-summary">
          <h2>本周小结</h2>
          <p class="sub">学习中 {{ learningCount }} 条</p>
          <p class="sub">已掌握 {{ masteredCount }} 条</p>
        </article>

        <section class="card notes-side-card notes-side-block">
          <h3>近 7 天发布</h3>
          <div class="notes-trend" role="img" aria-label="最近七天发布趋势">
            <div v-for="day in trendDays" :key="day.key" class="notes-trend-item">
              <div class="notes-trend-bar-wrap">
                <span class="notes-trend-bar" :style="{ height: `${day.height}%` }"></span>
              </div>
              <span class="notes-trend-count">{{ day.count }}</span>
              <span class="notes-trend-label">{{ day.label }}</span>
            </div>
          </div>
        </section>

        <section class="card notes-side-card notes-side-block">
          <h3>最近更新</h3>
          <ul class="notes-side-list">
            <li v-for="item in latestSideNotes" :key="item.id">
              <button class="notes-side-link" type="button" @click="goDetail(item.id)">{{ item.title }}</button>
              <p>{{ formatDateTime(noteDate(item)) }}</p>
            </li>
            <li v-if="!latestSideNotes.length" class="notes-side-empty">暂无内容</li>
          </ul>
        </section>

        <section class="card notes-side-card notes-side-block">
          <h3>标签 Top5</h3>
          <div class="notes-tag-cloud">
            <span v-for="tag in topTags" :key="tag.name" class="notes-tag-chip">{{ tag.name }} · {{ tag.count }}</span>
            <p v-if="!topTags.length" class="notes-side-empty">暂无标签</p>
          </div>
        </section>
      </aside>

      <article class="card notes-main">
        <div class="row page-head">
          <div>
            <h1>我的博客</h1>
            <p class="sub">先写，再打磨。</p>
          </div>
          <button class="primary-btn page-head-action" @click="goCreate">写新博客</button>
        </div>

        <form class="filters notes-toolbar" @submit.prevent="applyFilter">
          <div class="notes-search-box">
            <input v-model="query.keyword" placeholder="搜标题或内容" />
            <button class="notes-search-btn" type="submit" aria-label="搜索">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7"></circle>
                <path d="m20 20-3.5-3.5"></path>
              </svg>
            </button>
          </div>
          <button class="ghost-btn notes-filter-trigger" type="button" @click="openFilterModal">
            筛选
            <span v-if="activeFilterCount" class="notes-filter-count">{{ activeFilterCount }}</span>
          </button>
        </form>

        <div v-if="filterModalVisible" class="filter-modal-mask" @click="closeFilterModal">
          <div class="filter-modal" role="dialog" aria-modal="true" @click.stop>
            <div class="row filter-modal-head">
              <h3>筛选条件</h3>
              <button class="ghost-btn" type="button" @click="closeFilterModal">关闭</button>
            </div>
            <form class="form filter-modal-form" @submit.prevent="applyFilterFromModal">
              <section class="filter-group">
                <button class="filter-group-head" type="button" @click="toggleFilterBasic">
                  <span>基础筛选</span>
                  <span class="filter-group-arrow">{{ filterBasicExpanded ? '收起' : '展开' }}</span>
                </button>
                <div v-if="filterBasicExpanded" class="filter-group-body">
                  <div class="field">
                    <span>状态</span>
                    <select v-model="query.status">
                      <option value="">全部状态</option>
                      <option value="LEARNING">学习中</option>
                      <option value="MASTERED">已掌握</option>
                    </select>
                  </div>
                  <div class="field">
                    <span>正文关键词</span>
                    <input v-model="query.contentKeyword" placeholder="仅筛正文内容" />
                  </div>
                  <div class="field">
                    <span>开始日期</span>
                    <input v-model="query.startTime" type="date" />
                  </div>
                  <div class="field">
                    <span>结束日期</span>
                    <input v-model="query.endTime" type="date" />
                  </div>
                  <div class="field">
                    <span>最小阅读量</span>
                    <input v-model.number="query.minReadCount" type="number" min="0" placeholder="最小阅读量" />
                  </div>
                  <div class="field">
                    <span>最大阅读量</span>
                    <input v-model.number="query.maxReadCount" type="number" min="0" placeholder="最大阅读量" />
                  </div>
                </div>
              </section>

              <section class="filter-group">
                <button class="filter-group-head" type="button" @click="toggleFilterSort">
                  <span>排序设置</span>
                  <span class="filter-group-arrow">{{ filterSortExpanded ? '收起' : '展开' }}</span>
                </button>
                <div v-if="filterSortExpanded" class="filter-group-body">
                  <div class="field">
                    <span>排序字段</span>
                    <select v-model="query.sortBy">
                      <option value="updatedAt">按时间</option>
                      <option value="readCount">按阅读量</option>
                      <option value="contentLength">按内容长度</option>
                    </select>
                  </div>
                  <div class="field">
                    <span>排序方向</span>
                    <select v-model="query.sortOrder">
                      <option value="desc">降序</option>
                      <option value="asc">升序</option>
                    </select>
                  </div>
                </div>
              </section>

              <div class="row filter-modal-actions">
                <button class="ghost-btn" type="button" @click="clearAndReloadFilters">重置并应用</button>
                <button class="primary-btn" type="submit">应用筛选</button>
              </div>
            </form>
          </div>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-else-if="success" class="sub">{{ success }}</p>
        <p v-else-if="loading" class="sub">正在加载...</p>

        <div v-else-if="notes.length" class="notes-card-grid">
          <article
            v-for="(item, index) in notes"
            :key="item.id"
            class="notes-card"
            @click="goDetail(item.id)"
          >
            <img class="notes-card-cover" :src="coverByIndex(index)" :alt="item.title" loading="lazy" />
            <div class="notes-card-body">
              <p class="notes-card-time">发布于 {{ formatDateTime(noteDate(item)) }}</p>
              <h3 class="notes-card-title">{{ item.title }}</h3>
              <p class="notes-card-meta">{{ firstTag(item) }} · {{ statusText(item.status) }}</p>
              <p class="sub notes-card-excerpt">{{ notePreview(item.content) }}</p>
              <div v-if="authStore.user" class="row small-gap note-actions" @click.stop>
                <button class="ghost-btn" type="button" @click="router.push(`/notes/${item.id}/edit`)">编辑</button>
                <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
              </div>
            </div>
          </article>
        </div>

        <p v-else class="sub">{{ hasActiveFilter ? '没有匹配的笔记。' : '还没笔记，先写第一篇。' }}</p>

        <div class="row footer-bar notes-footer">
          <span class="sub">共 {{ total }} 条</span>
          <div class="row small-gap pager-actions">
            <button class="ghost-btn" type="button" :disabled="isPrevDisabled" @click="goPrev">上一页</button>
            <span class="sub pager-indicator">第 {{ query.page }} / {{ totalPages }} 页</span>
            <button class="ghost-btn" type="button" :disabled="isNextDisabled" @click="goNext">下一页</button>
          </div>
        </div>
      </article>
    </section>
  </section>
</template>

