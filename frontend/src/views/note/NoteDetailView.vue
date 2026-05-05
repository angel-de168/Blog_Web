<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createNoteComment, deleteNoteComment, fetchNote, fetchNoteComments } from '../../api/note'
import { useAuthStore } from '../../stores/auth'
import type { NoteComment, NoteItem } from '../../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const error = ref('')
const note = ref<NoteItem | null>(null)
const comments = ref<NoteComment[]>([])
const commentsLoading = ref(false)
const commentSaving = ref(false)
const commentInput = ref('')
const commentError = ref('')

const statusLabel = computed(() => ({
  LEARNING: '学习中',
  MASTERED: '已掌握'
}))

const heroImage = computed(() => {
  if (note.value?.coverImage?.trim()) {
    return note.value.coverImage
  }
  const seed = note.value?.id ?? 1025
  return `https://picsum.photos/seed/note-${seed}/2200/1200`
})

const authorName = computed(() => note.value?.authorName || '作者')
const publishTime = computed(() => {
  if (!note.value) {
    return ''
  }
  return formatDateTime(note.value.createdAt)
})

const readCount = computed(() => note.value?.readCount ?? 0)
const commentCount = computed(() => note.value?.commentCount ?? comments.value.length)

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

async function loadNote() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    error.value = '博客不存在'
    return
  }

  loading.value = true
  error.value = ''
  note.value = null
  try {
    note.value = await fetchNote(id)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    return
  }

  commentsLoading.value = true
  commentError.value = ''
  try {
    const res = await fetchNoteComments(id, 1, 30)
    comments.value = res.records
  } catch (e) {
    commentError.value = (e as Error).message
  } finally {
    commentsLoading.value = false
  }
}

async function submitComment() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    return
  }

  if (!authStore.user) {
    commentError.value = '请登录后使用'
    return
  }

  const content = commentInput.value.trim()
  if (!content) {
    commentError.value = '评论内容必填'
    return
  }

  commentSaving.value = true
  commentError.value = ''
  try {
    await createNoteComment(id, { content })
    commentInput.value = ''
    await loadComments()
    if (note.value) {
      note.value.commentCount = comments.value.length
    }
  } catch (e) {
    commentError.value = (e as Error).message
  } finally {
    commentSaving.value = false
  }
}

async function removeComment(commentId: number) {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    return
  }

  if (!authStore.user) {
    commentError.value = '请登录后使用'
    return
  }

  commentError.value = ''
  try {
    await deleteNoteComment(id, commentId)
    await loadComments()
    if (note.value) {
      note.value.commentCount = comments.value.length
    }
  } catch (e) {
    commentError.value = (e as Error).message
  }
}

function goLoginForComment() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    void router.push('/login')
    return
  }
  void router.push({ name: 'login', query: { redirect: `/notes/${id}` } })
}

function goNotes() {
  void router.push('/notes')
}

onMounted(async () => {
  await loadNote()
  await loadComments()
})
</script>

<template>
  <section class="page-scene note-detail-scene">
    <header class="page-hero note-detail-hero" :style="{ '--page-hero-image': `url(${heroImage})` }">
      <div v-if="note" class="page-hero-copy note-detail-hero-copy">
        <h1>{{ note.title }}</h1>
        <p class="note-detail-head-meta">
          {{ authorName }} · 发布于 {{ publishTime }} · 阅读 {{ readCount }} · 评论 {{ commentCount }}
        </p>
        <p class="note-detail-tags">
          <span v-for="tag in note.tags" :key="tag" class="tag muted">{{ tag }}</span>
          <span class="tag">{{ statusLabel[note.status] }}</span>
        </p>
      </div>
    </header>

    <section class="page-grid note-detail-grid">
      <article class="card note-detail-main">
        <div v-if="loading" class="sub">正在加载博客内容...</div>
        <p v-else-if="error" class="error">{{ error }}</p>

        <template v-else-if="note">
          <article class="note-detail-content">{{ note.content }}</article>

          <section class="note-detail-comments">
            <div class="row note-detail-comments-head">
              <h2>评论区</h2>
              <button class="ghost-btn" type="button" @click="goNotes">返回博客列表</button>
            </div>

            <form class="form note-detail-comment-form" @submit.prevent="submitComment">
              <label class="field">
                <span>写评论</span>
                <textarea v-model="commentInput" rows="3" placeholder="写下你的想法" />
              </label>
              <div class="row note-detail-comment-actions">
                <button class="primary-btn" type="submit" :disabled="commentSaving">
                  {{ commentSaving ? '发布中...' : '发布评论' }}
                </button>
                <button v-if="!authStore.user" class="ghost-btn" type="button" @click="goLoginForComment">去登录</button>
              </div>
            </form>

            <p v-if="commentError" class="error">{{ commentError }}</p>
            <p v-if="commentsLoading" class="sub">评论加载中...</p>
            <p v-else-if="!comments.length" class="sub">还没有评论。</p>

            <ul v-else class="note-comment-list">
              <li v-for="item in comments" :key="item.id" class="note-comment-item">
                <div class="row note-comment-head">
                  <p class="note-comment-user">{{ item.username }}</p>
                  <span class="sub">{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <p class="note-comment-content">{{ item.content }}</p>
                <button
                  v-if="authStore.user && authStore.user.id === item.userId"
                  class="danger-btn"
                  type="button"
                  @click="removeComment(item.id)"
                >
                  删除评论
                </button>
              </li>
            </ul>
          </section>
        </template>
      </article>
    </section>
  </section>
</template>
