<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  createMomentComment,
  deleteMoment,
  deleteMomentComment,
  fetchMomentComments,
  fetchMoments,
  likeMoment,
  unlikeMoment
} from '../../api/moment'
import { getMainUser } from '../../api/auth'
import { followUser, fetchFollowStatus, unfollowUser } from '../../api/follow'
import type { MomentComment, MomentItem, MomentQuery, UserProfile } from '../../types'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const query = reactive({
  keyword: '',
  mood: '',
  contentKeyword: '',
  startTime: '',
  endTime: '',
  minLikeCount: null as number | null,
  maxLikeCount: null as number | null,
  visibilityScope: '' as '' | 'PUBLIC' | 'PRIVATE_SELF' | 'CIRCLE',
  sortBy: 'updatedAt' as 'updatedAt' | 'likeCount' | 'commentCount' | 'contentLength',
  sortOrder: 'desc' as 'asc' | 'desc',
  page: 1,
  size: 10
})

const total = ref(0)
const loading = ref(false)
const moments = ref<MomentItem[]>([])
const error = ref('')
const success = ref('')
const heroImage = 'https://picsum.photos/id/1025/2200/1200'
const mainUser = ref<UserProfile | null>(null)

const createNoticeVisible = ref(false)
const filterModalVisible = ref(false)
const filterBasicExpanded = ref(true)
const filterSortExpanded = ref(false)

const followLoading = ref(false)
const followedByCurrentUser = ref(false)

const commentsByMoment = ref<Record<number, MomentComment[]>>({})
const commentInput = ref<Record<number, string>>({})
const commentLoading = ref<Record<number, boolean>>({})
const commentSaving = ref<Record<number, boolean>>({})
const likeLoading = ref<Record<number, boolean>>({})
const commentsExpanded = ref<Record<number, boolean>>({})
const actionHintByMoment = ref<Record<number, string>>({})
const actionHintTimerByMoment = new Map<number, number>()

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const isPrevDisabled = computed(() => loading.value || query.page <= 1)
const isNextDisabled = computed(() => loading.value || query.page >= totalPages.value)
const momentAuthorName = computed(() => mainUser.value?.username?.trim() || '知行记录者')
const momentAuthorAvatar = computed(() => mainUser.value?.avatar?.trim() || '')
const momentAuthorLetter = computed(() => momentAuthorName.value.charAt(0) || '知')
const mainUserId = computed(() => mainUser.value?.id ?? 0)
const isSelfMainUser = computed(() => !!authStore.user && authStore.user.id === mainUserId.value)

const hasActiveFilter = computed(
  () =>
    !!query.keyword.trim() ||
    !!query.mood.trim() ||
    !!query.contentKeyword.trim() ||
    !!query.startTime.trim() ||
    !!query.endTime.trim() ||
    query.minLikeCount !== null ||
    query.maxLikeCount !== null ||
    !!query.visibilityScope ||
    query.sortBy !== 'updatedAt' ||
    query.sortOrder !== 'desc'
)

const activeFilterCount = computed(() => {
  let count = 0
  if (query.mood.trim()) {
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
  if (query.minLikeCount !== null) {
    count += 1
  }
  if (query.maxLikeCount !== null) {
    count += 1
  }
  if (query.visibilityScope) {
    count += 1
  }
  if (query.sortBy !== 'updatedAt' || query.sortOrder !== 'desc') {
    count += 1
  }
  return count
})

function normalizeCount(value: number | string | null) {
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
  query.mood = query.mood.trim()
  query.contentKeyword = query.contentKeyword.trim()
  query.startTime = query.startTime.trim()
  query.endTime = query.endTime.trim()
  query.minLikeCount = normalizeCount(query.minLikeCount)
  query.maxLikeCount = normalizeCount(query.maxLikeCount)
}

function buildQueryParams(): MomentQuery {
  normalizeFilterValues()
  return {
    keyword: query.keyword || undefined,
    mood: query.mood || undefined,
    contentKeyword: query.contentKeyword || undefined,
    visibilityScope: query.visibilityScope || undefined,
    startTime: query.startTime ? `${query.startTime}T00:00:00` : undefined,
    endTime: query.endTime ? `${query.endTime}T23:59:59` : undefined,
    minLikeCount: query.minLikeCount ?? undefined,
    maxLikeCount: query.maxLikeCount ?? undefined,
    sortBy: query.sortBy,
    sortOrder: query.sortOrder,
    page: query.page,
    size: query.size
  }
}

function openFilterModal() {
  filterModalVisible.value = true
}

function closeFilterModal() {
  filterModalVisible.value = false
}

function toggleFilterBasic() {
  filterBasicExpanded.value = !filterBasicExpanded.value
}

function toggleFilterSort() {
  filterSortExpanded.value = !filterSortExpanded.value
}

function resetFilters() {
  query.mood = ''
  query.contentKeyword = ''
  query.startTime = ''
  query.endTime = ''
  query.minLikeCount = null
  query.maxLikeCount = null
  query.visibilityScope = ''
  query.sortBy = 'updatedAt'
  query.sortOrder = 'desc'
}

async function applyFilterFromModal() {
  query.page = 1
  await loadData()
  closeFilterModal()
}

async function clearAndReloadFilters() {
  resetFilters()
  query.page = 1
  await loadData()
  closeFilterModal()
}

function patchMoment(next: MomentItem) {
  const index = moments.value.findIndex((item) => item.id === next.id)
  if (index === -1) {
    return
  }
  moments.value[index] = next
}

function formatDateTime(value: string) {
  if (!value) {
    return ''
  }
  return value.slice(0, 16).replace('T', ' ')
}

async function loadMainUser() {
  try {
    mainUser.value = await getMainUser()
  } catch {
    mainUser.value = null
  }
}

async function loadFollowStatus() {
  if (!authStore.user || !mainUserId.value || isSelfMainUser.value) {
    followedByCurrentUser.value = false
    return
  }
  followedByCurrentUser.value = await fetchFollowStatus(mainUserId.value)
}

async function toggleFollowMainUser() {
  if (!mainUserId.value) {
    return
  }
  if (!authStore.user) {
    void router.push({ name: 'login', query: { redirect: '/moments' } })
    return
  }
  if (isSelfMainUser.value) {
    return
  }

  followLoading.value = true
  error.value = ''
  success.value = ''
  try {
    if (followedByCurrentUser.value) {
      await unfollowUser(mainUserId.value)
      followedByCurrentUser.value = false
      success.value = '已取消关注'
    } else {
      await followUser(mainUserId.value)
      followedByCurrentUser.value = true
      success.value = '关注成功'
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    followLoading.value = false
  }
}

function showMomentActionHint(momentId: number, message: string) {
  actionHintByMoment.value[momentId] = message
  const prevTimer = actionHintTimerByMoment.get(momentId)
  if (prevTimer) {
    window.clearTimeout(prevTimer)
  }
  const timer = window.setTimeout(() => {
    delete actionHintByMoment.value[momentId]
    actionHintTimerByMoment.delete(momentId)
  }, 1800)
  actionHintTimerByMoment.set(momentId, timer)
}


function requireAuthFor(action: '点赞' | '评论', momentId?: number) {
  if (authStore.user) {
    return true
  }
  if (typeof momentId === 'number') {
    showMomentActionHint(momentId, `登录后才能${action}`)
    return false
  }
  error.value = `登录后才能${action}`
  return false
}


function goCreateMoment() {
  if (authStore.user) {
    void router.push('/moments/new')
    return
  }
  createNoticeVisible.value = true
}

function closeCreateNotice() {
  createNoticeVisible.value = false
}

function goLoginForCreate() {
  createNoticeVisible.value = false
  void router.push({ name: 'login', query: { redirect: '/moments/new' } })
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

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchMoments(buildQueryParams())
    moments.value = res.records
    total.value = res.total
    query.page = res.page
    query.size = res.size
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function applyFilter() {
  query.page = 1
  await loadData()
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

async function remove(id: number) {
  if (!authStore.user) {
    error.value = '请登录后使用'
    return
  }
  await deleteMoment(id)
  if (moments.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadData()
}

async function toggleLike(item: MomentItem) {
  if (!requireAuthFor('点赞', item.id)) {
    return
  }
  likeLoading.value[item.id] = true
  error.value = ''
  try {
    const next = item.likedByCurrentUser ? await unlikeMoment(item.id) : await likeMoment(item.id)
    patchMoment(next)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    likeLoading.value[item.id] = false
  }
}

async function loadComments(momentId: number) {
  commentLoading.value[momentId] = true
  error.value = ''
  try {
    const res = await fetchMomentComments(momentId, 1, 20)
    commentsByMoment.value[momentId] = res.records
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    commentLoading.value[momentId] = false
  }
}

async function toggleComments(item: MomentItem) {
  commentsExpanded.value[item.id] = !commentsExpanded.value[item.id]
  if (commentsExpanded.value[item.id] && !commentsByMoment.value[item.id]) {
    await loadComments(item.id)
  }
}

async function submitComment(momentId: number) {
  if (!requireAuthFor('评论', momentId)) {
    return
  }

  const content = (commentInput.value[momentId] || '').trim()
  if (!content) {
    error.value = '评论内容必填'
    return
  }

  commentSaving.value[momentId] = true
  error.value = ''
  try {
    await createMomentComment(momentId, { content })
    commentInput.value[momentId] = ''
    await loadComments(momentId)
    const next = await fetchMoments(buildQueryParams())
    moments.value = next.records
    total.value = next.total
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    commentSaving.value[momentId] = false
  }
}

async function removeComment(momentId: number, commentId: number) {
  if (!requireAuthFor('评论', momentId)) {
    return
  }

  error.value = ''
  try {
    await deleteMomentComment(momentId, commentId)
    await loadComments(momentId)
    const next = await fetchMoments(buildQueryParams())
    moments.value = next.records
    total.value = next.total
  } catch (e) {
    error.value = (e as Error).message
  }
}

watch(
  () => [authStore.user?.id, mainUserId.value],
  () => {
    void loadFollowStatus()
  }
)

onMounted(async () => {
  applySuccessFromRoute()
  await Promise.all([loadData(), loadMainUser()])
  await loadFollowStatus()
})

onBeforeUnmount(() => {
  actionHintTimerByMoment.forEach((timer) => window.clearTimeout(timer))
  actionHintTimerByMoment.clear()
})
</script>

<template>
  <section class="page-scene moments-scene">
    <header class="page-hero" :style="{ '--page-hero-image': `url(${heroImage})` }">
      <div class="page-hero-copy">
        <p class="page-hero-kicker">MOMENT DIARY</p>
        <h1>我的随笔</h1>
        <p class="sub">想到就写，不必完整。</p>
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

    <section class="page-grid moments-grid single-main">
      <article class="card moments-main moments-main-centered">
        <div class="row page-head">
          <div>
            <h1>我的随笔</h1>
            <p class="sub">想到就写，不必完整。</p>
          </div>
          <button class="primary-btn page-head-action" @click="goCreateMoment">发随笔</button>
        </div>

        <form class="filters notes-toolbar moments-toolbar" @submit.prevent="applyFilter">
          <div class="notes-search-box">
            <input v-model="query.keyword" placeholder="搜内容或心情" />
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
                    <span>心情关键词</span>
                    <input v-model="query.mood" placeholder="例如：平静、兴奋" />
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
                    <span>最小点赞量</span>
                    <input v-model.number="query.minLikeCount" type="number" min="0" placeholder="最小点赞量" />
                  </div>
                  <div class="field">
                    <span>最大点赞量</span>
                    <input v-model.number="query.maxLikeCount" type="number" min="0" placeholder="最大点赞量" />
                  </div>
                  <div class="field">
                    <span>可见范围</span>
                    <select v-model="query.visibilityScope">
                      <option value="">全部</option>
                      <option value="PUBLIC">公开</option>
                      <option value="PRIVATE_SELF">仅自己</option>
                      <option value="CIRCLE">圈层</option>
                    </select>
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
                      <option value="likeCount">按点赞量</option>
                      <option value="commentCount">按评论量</option>
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
        <p v-if="loading" class="sub">正在加载...</p>
        <p v-else-if="!moments.length" class="sub">{{ hasActiveFilter ? '没有匹配的随笔。' : '还没有随笔，先写第一条。' }}</p>

        <ul v-if="!loading && moments.length" class="moment-list timeline-list">
          <li v-for="item in moments" :key="item.id" class="moment-item timeline-item">
            <div class="moment-main">
              <span class="moment-author-avatar-wrap" aria-hidden="true">
                <img v-if="momentAuthorAvatar" class="moment-author-avatar" :src="momentAuthorAvatar" :alt="momentAuthorName" />
                <span v-else class="moment-author-avatar-fallback">{{ momentAuthorLetter }}</span>
              </span>

              <div class="moment-main-body">
                <div class="meta moment-meta">
                  <span class="moment-author-name">{{ momentAuthorName }}</span>
                  <button
                    v-if="!isSelfMainUser"
                    class="ghost-btn"
                    type="button"
                    :disabled="followLoading || !mainUserId"
                    @click="toggleFollowMainUser"
                  >
                    {{ followLoading ? '处理中...' : followedByCurrentUser ? '取消关注' : '关注' }}
                  </button>
                </div>
                <p class="moment-content">{{ item.content }}</p>

                <section v-if="commentsExpanded[item.id]" class="moment-comments">
                  <form v-if="authStore.user" class="form moment-comment-form" @submit.prevent="submitComment(item.id)">
                    <label class="field">
                      <span>写评论</span>
                      <textarea v-model="commentInput[item.id]" rows="2" placeholder="写一句也好" />
                    </label>
                    <button class="primary-btn" type="submit" :disabled="commentSaving[item.id]">
                      {{ commentSaving[item.id] ? '发送中...' : '发送评论' }}
                    </button>
                  </form>
                  <p v-else class="sub">登录后才能评论。</p>

                  <p v-if="commentLoading[item.id]" class="sub">评论加载中...</p>
                  <p v-else-if="!(commentsByMoment[item.id] || []).length" class="sub">还没有评论。</p>

                  <ul v-else class="moment-comment-list">
                    <li v-for="comment in commentsByMoment[item.id]" :key="comment.id" class="moment-comment-item">
                      <div class="row moment-comment-head">
                        <p class="moment-comment-user">{{ comment.username }}</p>
                        <span class="sub">{{ formatDateTime(comment.createdAt) }}</span>
                      </div>
                      <p class="moment-comment-content">{{ comment.content }}</p>
                      <button
                        v-if="authStore.user && authStore.user.id === comment.userId"
                        class="danger-btn"
                        type="button"
                        @click="removeComment(item.id, comment.id)"
                      >
                        删除评论
                      </button>
                    </li>
                  </ul>
                </section>

                <div class="moment-entry-footer">
                  <span class="sub moment-time-bottom">{{ formatDateTime(item.updatedAt) }}</span>
                  <div class="row small-gap moment-social-row">
                    <button
                      class="ghost-btn moment-action-icon"
                      type="button"
                      :disabled="likeLoading[item.id]"
                      :aria-label="authStore.user ? (item.likedByCurrentUser ? '取消点赞' : '点赞') : '登录后点赞'"
                      @click="toggleLike(item)"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path
                          d="M12 20.7c-.2 0-.5-.1-.7-.2C7 17.9 4 15.3 4 11.5 4 9.1 5.9 7.2 8.3 7.2c1.4 0 2.7.7 3.5 1.8.8-1.1 2.1-1.8 3.5-1.8 2.4 0 4.3 1.9 4.3 4.3 0 3.8-3 6.4-7.3 9-.2.1-.4.2-.6.2z"
                        />
                      </svg>
                      <span>{{ item.likeCount }}</span>
                    </button>
                    <button
                      class="ghost-btn moment-action-icon"
                      type="button"
                      :disabled="commentLoading[item.id]"
                      :aria-label="commentsExpanded[item.id] ? '收起评论' : '查看评论'"
                      @click="toggleComments(item)"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path
                          d="M5.5 6.5h13a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-7l-3.8 3v-3H5.5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z"
                        />
                      </svg>
                      <span>{{ item.commentCount }}</span>
                    </button>
                    <template v-if="authStore.user">
                      <button class="ghost-btn" type="button" @click="router.push(`/moments/${item.id}/edit`)">编辑</button>
                      <button class="danger-btn" type="button" @click="remove(item.id)">删除</button>
                    </template>
                  </div>
                </div>
                <p v-if="actionHintByMoment[item.id]" class="moment-inline-hint">{{ actionHintByMoment[item.id] }}</p>
              </div>
            </div>
          </li>
        </ul>

        <div class="row footer-bar moments-footer">
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
