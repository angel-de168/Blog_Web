<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createOrGetSession, fetchPrivateSessions } from '../../api/message'
import type { PrivateSessionItem } from '../../types'

const router = useRouter()

const query = reactive({
  page: 1,
  size: 10
})

const total = ref(0)
const loading = ref(false)
const error = ref('')
const sessions = ref<PrivateSessionItem[]>([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const isPrevDisabled = computed(() => loading.value || query.page <= 1)
const isNextDisabled = computed(() => loading.value || query.page >= totalPages.value)

function formatDateTime(value?: string | null) {
  if (!value) {
    return '暂无消息'
  }
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

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchPrivateSessions(query.page, query.size)
    sessions.value = res.records
    total.value = res.total
    query.page = res.page
    query.size = res.size
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function openSession(item: PrivateSessionItem) {
  void router.push(`/messages/${item.id}`)
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

async function quickStartWithMainUser() {
  error.value = ''
  try {
    const session = await createOrGetSession({ targetUserId: 3 })
    void router.push(`/messages/${session.id}`)
  } catch (e) {
    error.value = (e as Error).message
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-grid">
    <article class="card">
      <div class="row page-head">
        <div>
          <h1>我的私信</h1>
          <p class="sub">把想说的话，稳稳落在这里。</p>
        </div>
        <button class="ghost-btn" type="button" @click="quickStartWithMainUser">给主用户发消息</button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="sub">正在加载会话...</p>
      <p v-else-if="!sessions.length" class="sub">还没有私信会话。</p>

      <ul v-if="!loading && sessions.length" class="moment-list">
        <li v-for="item in sessions" :key="item.id" class="moment-item">
          <button class="moment-main message-session-btn" type="button" @click="openSession(item)">
            <span class="moment-author-avatar-wrap" aria-hidden="true">
              <img v-if="item.peerAvatar" class="moment-author-avatar" :src="item.peerAvatar" :alt="item.peerUsername" />
              <span v-else class="moment-author-avatar-fallback">{{ item.peerUsername.charAt(0) || '友' }}</span>
            </span>

            <div class="moment-main-body">
              <div class="row">
                <div>
                  <p class="moment-author-name">{{ item.peerUsername }}</p>
                  <p class="sub">{{ item.lastMessage || '还没有消息，点进去聊聊。' }}</p>
                </div>
                <span class="sub">{{ formatDateTime(item.lastMessageAt) }}</span>
              </div>
            </div>
          </button>
        </li>
      </ul>

      <div class="row footer-bar moments-footer">
        <span class="sub">共 {{ total }} 个会话</span>
        <div class="row small-gap pager-actions">
          <button class="ghost-btn" type="button" :disabled="isPrevDisabled" @click="goPrev">上一页</button>
          <span class="sub pager-indicator">第 {{ query.page }} / {{ totalPages }} 页</span>
          <button class="ghost-btn" type="button" :disabled="isNextDisabled" @click="goNext">下一页</button>
        </div>
      </div>
    </article>
  </section>
</template>
