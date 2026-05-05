<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchNotes } from '../../api/note'
import { getMainUser } from '../../api/auth'
import { followUser, fetchFollowStatus, unfollowUser } from '../../api/follow'
import { getSiteStats } from '../../api/site'
import { useAuthStore } from '../../stores/auth'
import type { NoteItem, SiteStats, UserProfile } from '../../types'

const router = useRouter()
const authStore = useAuthStore()
const notice = ref('')
const mainUser = ref<UserProfile | null>(null)
const siteStats = ref<SiteStats>({ noteCount: 0, tagCount: 0, visitCount: 0 })

const heroImage = 'https://picsum.photos/id/1039/1800/980'
const latestNotes = ref<NoteItem[]>([])
const latestLoading = ref(false)
const latestError = ref('')
const profileLoading = ref(false)
const profileError = ref('')
const statsLoading = ref(false)
const statsError = ref('')
const followLoading = ref(false)
const followedByCurrentUser = ref(false)

const mainUserId = computed(() => mainUser.value?.id ?? 0)
const isSelfMainUser = computed(() => !!authStore.user && authStore.user.id === mainUserId.value)

const cardCovers = [
  'https://picsum.photos/id/1027/860/560',
  'https://picsum.photos/id/1050/860/560',
  'https://picsum.photos/id/1060/860/560',
  'https://picsum.photos/id/1035/860/560'
]

async function goFeature(path: string) {
  notice.value = ''
  await router.push(path)
}

async function loadLatestNotes() {
  latestError.value = ''
  latestLoading.value = true
  try {
    const res = await fetchNotes({ page: 1, size: 4 })
    latestNotes.value = res.records
  } catch (e) {
    latestError.value = (e as Error).message
  } finally {
    latestLoading.value = false
  }
}

async function loadMainUser() {
  profileError.value = ''
  profileLoading.value = true
  try {
    mainUser.value = await getMainUser()
  } catch (e) {
    profileError.value = (e as Error).message
  } finally {
    profileLoading.value = false
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
    await router.push({ name: 'login', query: { redirect: '/' } })
    return
  }
  if (isSelfMainUser.value) {
    return
  }

  followLoading.value = true
  notice.value = ''
  try {
    if (followedByCurrentUser.value) {
      await unfollowUser(mainUserId.value)
      followedByCurrentUser.value = false
      notice.value = '已取消关注'
    } else {
      await followUser(mainUserId.value)
      followedByCurrentUser.value = true
      notice.value = '关注成功'
    }
  } catch (e) {
    notice.value = (e as Error).message
  } finally {
    followLoading.value = false
  }
}

async function loadSiteStats() {
  statsError.value = ''
  statsLoading.value = true
  try {
    siteStats.value = await getSiteStats()
  } catch (e) {
    statsError.value = (e as Error).message
  } finally {
    statsLoading.value = false
  }
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

function firstTag(note: NoteItem) {
  return note.tags[0] || '未分类'
}

function noteDate(note: NoteItem) {
  return note.updatedAt || note.createdAt
}

function coverByIndex(index: number) {
  return cardCovers[index % cardCovers.length]
}

function goCreateNote() {
  void router.push('/notes/new')
}

function goNoteDetail(id: number) {
  void router.push(`/notes/${id}`)
}

watch(
  () => [authStore.user?.id, mainUserId.value],
  () => {
    void loadFollowStatus()
  }
)

onMounted(async () => {
  await Promise.all([loadMainUser(), loadSiteStats(), loadLatestNotes()])
  await loadFollowStatus()
})
</script>

<template>
  <section class="home-wrap">
    <article class="home-hero" :style="{ '--hero-image': `url(${heroImage})` }">
      <div class="home-hero-overlay">
        <p class="home-kicker">SEA OF NOTES</p>
        <h1>写给未来的博客</h1>
        <p class="home-sub">把感悟写下来，把成长看清楚。</p>
        <p v-if="notice" class="home-notice">{{ notice }}</p>
      </div>
      <div class="home-arrow">⌄</div>
    </article>

    <div class="home-wave" aria-hidden="true"></div>

    <section class="home-main">
      <aside class="home-profile-card">
        <div class="home-avatar-wrap">
          <img v-if="mainUser?.avatar" class="home-avatar-img" :src="mainUser.avatar" :alt="mainUser.username" />
          <div v-else class="home-avatar">{{ mainUser?.username?.trim().charAt(0) || '知' }}</div>
        </div>
        <h2>知行记录者</h2>
        <p v-if="profileLoading">正在加载主用户资料...</p>
        <p v-else-if="profileError">{{ profileError }}</p>
        <p v-else>记录真实成长，保持长期写作。</p>

        <button
          v-if="!isSelfMainUser"
          class="primary-btn"
          type="button"
          :disabled="followLoading || !mainUserId"
          @click="toggleFollowMainUser"
        >
          {{ followLoading ? '处理中...' : followedByCurrentUser ? '取消关注' : '关注' }}
        </button>

        <div class="home-stats">
          <div>
            <strong>文章</strong>
            <span>{{ statsLoading ? '...' : siteStats.noteCount }}</span>
            <small>博客数量</small>
          </div>
          <div>
            <strong>标签</strong>
            <span>{{ statsLoading ? '...' : siteStats.tagCount }}</span>
            <small>博客标签数量（去重）</small>
          </div>
          <div>
            <strong>访问量</strong>
            <span>{{ statsLoading ? '...' : siteStats.visitCount }}</span>
            <small>本网站访问量</small>
          </div>
        </div>

        <p v-if="statsError" class="home-stats-error">{{ statsError }}</p>
      </aside>

      <section class="home-feed">
        <article class="home-announcement">
          <h3>站内公告</h3>
          <p>先写博客，再慢慢改。每次打开，都留下新一段。</p>
        </article>

        <section class="home-latest">
          <header class="home-latest-head">
            <p class="home-latest-title">最新</p>
            <button class="home-latest-more" type="button" @click="goFeature('/notes')">查看全部</button>
          </header>

          <p v-if="latestLoading" class="home-latest-empty">正在加载最新博客...</p>
          <p v-else-if="latestError" class="home-latest-empty">{{ latestError }}</p>

          <div v-else-if="latestNotes.length" class="home-latest-grid">
            <article
              v-for="(item, index) in latestNotes"
              :key="item.id"
              class="home-latest-card"
              @click="goNoteDetail(item.id)"
            >
              <img class="home-latest-cover" :src="coverByIndex(index)" :alt="item.title" loading="lazy" />
              <div class="home-latest-body">
                <p class="home-latest-time">发布于 {{ formatDateTime(noteDate(item)) }}</p>
                <h3 class="home-latest-name">{{ item.title }}</h3>
                <p class="home-latest-meta">{{ firstTag(item) }} · {{ statusText(item.status) }}</p>
              </div>
            </article>
          </div>

          <div v-else class="home-latest-empty-wrap">
            <p class="home-latest-empty">还没有博客内容。</p>
            <button class="home-latest-create" type="button" @click="goCreateNote">去写博客</button>
          </div>
        </section>
      </section>
    </section>
  </section>
</template>
