<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchFollowing, unfollowUser } from '../../api/follow'
import { createOrGetSession } from '../../api/message'
import type { FollowUserItem } from '../../types'

const router = useRouter()

const query = reactive({
  page: 1,
  size: 10
})

const total = ref(0)
const loading = ref(false)
const error = ref('')
const users = ref<FollowUserItem[]>([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const isPrevDisabled = computed(() => loading.value || query.page <= 1)
const isNextDisabled = computed(() => loading.value || query.page >= totalPages.value)

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchFollowing(query.page, query.size)
    users.value = res.records
    total.value = res.total
    query.page = res.page
    query.size = res.size
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function removeFollow(targetUserId: number) {
  error.value = ''
  try {
    await unfollowUser(targetUserId)
    if (users.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await loadData()
  } catch (e) {
    error.value = (e as Error).message
  }
}

async function openMessage(targetUserId: number) {
  error.value = ''
  try {
    const session = await createOrGetSession({ targetUserId })
    void router.push(`/messages/${session.id}`)
  } catch (e) {
    error.value = (e as Error).message
  }
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

function goFollowers() {
  void router.push('/followers')
}

onMounted(loadData)
</script>

<template>
  <section class="page-grid">
    <article class="card">
      <div class="row page-head">
        <div>
          <h1>我的关注</h1>
          <p class="sub">你主动连接的人，都在这里。</p>
        </div>
        <button class="ghost-btn" type="button" @click="goFollowers">看关注我的人</button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="sub">正在加载...</p>
      <p v-else-if="!users.length" class="sub">还没有关注任何人。</p>

      <ul v-if="!loading && users.length" class="moment-list">
        <li v-for="item in users" :key="item.id" class="moment-item">
          <div class="moment-main">
            <span class="moment-author-avatar-wrap" aria-hidden="true">
              <img v-if="item.avatar" class="moment-author-avatar" :src="item.avatar" :alt="item.username" />
              <span v-else class="moment-author-avatar-fallback">{{ item.username.charAt(0) || '友' }}</span>
            </span>
            <div class="moment-main-body">
              <div class="row">
                <div>
                  <p class="moment-author-name">{{ item.username }}</p>
                  <p class="sub">{{ item.bio || '这个人还没有留下简介。' }}</p>
                </div>
                <div class="row small-gap">
                  <button class="ghost-btn" type="button" @click="openMessage(item.id)">发私信</button>
                  <button class="danger-btn" type="button" @click="removeFollow(item.id)">取关</button>
                </div>
              </div>
            </div>
          </div>
        </li>
      </ul>

      <div class="row footer-bar moments-footer">
        <span class="sub">共 {{ total }} 人</span>
        <div class="row small-gap pager-actions">
          <button class="ghost-btn" type="button" :disabled="isPrevDisabled" @click="goPrev">上一页</button>
          <span class="sub pager-indicator">第 {{ query.page }} / {{ totalPages }} 页</span>
          <button class="ghost-btn" type="button" :disabled="isNextDisabled" @click="goNext">下一页</button>
        </div>
      </div>
    </article>
  </section>
</template>
