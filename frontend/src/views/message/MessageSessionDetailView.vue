<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPrivateMessages, sendPrivateMessage } from '../../api/message'
import { useAuthStore } from '../../stores/auth'
import type { PrivateMessageItem } from '../../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const query = reactive({
  page: 1,
  size: 20
})

const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const input = ref('')
const messages = ref<PrivateMessageItem[]>([])

const sessionId = computed(() => Number(route.params.id))

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

async function loadData() {
  if (!Number.isFinite(sessionId.value) || sessionId.value <= 0) {
    error.value = '会话不存在'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const res = await fetchPrivateMessages(sessionId.value, query.page, query.size)
    messages.value = [...res.records].reverse()
    total.value = res.total
    query.page = res.page
    query.size = res.size
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function submitMessage() {
  if (!Number.isFinite(sessionId.value) || sessionId.value <= 0) {
    return
  }

  const content = input.value.trim()
  if (!content) {
    error.value = '消息内容必填'
    return
  }

  saving.value = true
  error.value = ''
  try {
    const created = await sendPrivateMessage(sessionId.value, { content })
    input.value = ''
    messages.value.push(created)
    total.value += 1
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    saving.value = false
  }
}

function goSessionList() {
  void router.push('/messages')
}

onMounted(loadData)
</script>

<template>
  <section class="page-grid">
    <article class="card">
      <div class="row page-head">
        <div>
          <h1>会话详情</h1>
          <p class="sub">慢慢聊，别着急。</p>
        </div>
        <button class="ghost-btn" type="button" @click="goSessionList">返回会话列表</button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="sub">正在加载消息...</p>
      <p v-else-if="!messages.length" class="sub">还没有消息，先发第一句。</p>

      <ul v-if="!loading && messages.length" class="note-comment-list">
        <li
          v-for="item in messages"
          :key="item.id"
          class="note-comment-item"
          :class="{ 'message-self': authStore.user && authStore.user.id === item.senderUserId }"
        >
          <div class="row note-comment-head">
            <p class="note-comment-user">{{ item.senderUsername }}</p>
            <span class="sub">{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <p class="note-comment-content">{{ item.content }}</p>
        </li>
      </ul>

      <form class="form note-detail-comment-form" @submit.prevent="submitMessage">
        <label class="field">
          <span>发消息</span>
          <textarea v-model="input" rows="3" placeholder="写下你想说的话" />
        </label>
        <div class="row note-detail-comment-actions">
          <button class="primary-btn" type="submit" :disabled="saving">
            {{ saving ? '发送中...' : '发送消息' }}
          </button>
        </div>
      </form>
    </article>
  </section>
</template>
