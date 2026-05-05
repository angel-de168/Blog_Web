<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchMoment, saveMoment } from '../../api/moment'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')

const form = reactive({
  id: undefined as number | undefined,
  content: '',
  mood: '',
  visibilityScope: 'PUBLIC' as 'PUBLIC' | 'PRIVATE_SELF' | 'CIRCLE'
})

const isEdit = computed(() => Boolean(route.params.id))
const contentLength = computed(() => form.content.trim().length)

async function loadDetail() {
  if (!isEdit.value) {
    return
  }

  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    const moment = await fetchMoment(id)
    form.id = moment.id
    form.content = moment.content
    form.mood = moment.mood || ''
    if (moment.visibilityScope === 'PRIVATE_SELF' || moment.visibilityScope === 'CIRCLE') {
      form.visibilityScope = moment.visibilityScope
    } else {
      form.visibilityScope = 'PUBLIC'
    }
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.content) {
    error.value = '内容必填'
    return
  }

  loading.value = true
  try {
    await saveMoment({
      id: form.id,
      content: form.content,
      mood: form.mood,
      visibilityScope: form.visibilityScope
    })
    await router.push({ path: '/moments', query: { saved: isEdit.value ? 'updated' : 'created' } })
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <section class="page-grid editor-grid">
    <article class="card editor-main">
      <div class="editor-head">
        <h1>{{ isEdit ? '修改这条随笔' : '写下这一刻' }}</h1>
        <p class="sub">一句也好，先记住此刻。</p>
      </div>

      <form class="form" @submit.prevent="submit">
        <label class="field">
          <span>心情</span>
          <input v-model.trim="form.mood" placeholder="比如：平静、兴奋、轻松" />
        </label>

        <label class="field">
          <span>可见范围</span>
          <select v-model="form.visibilityScope">
            <option value="PUBLIC">公开</option>
            <option value="PRIVATE_SELF">仅自己</option>
            <option value="CIRCLE">圈层</option>
          </select>
        </label>

        <label class="field">
          <span>内容</span>
          <textarea v-model="form.content" rows="12" placeholder="写点今天的感悟、心情或日记。" />
        </label>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="row">
          <button class="primary-btn" :disabled="loading" type="submit">{{ loading ? '保存中...' : '发布' }}</button>
          <button class="ghost-btn" type="button" @click="router.push('/moments')">取消</button>
        </div>
      </form>
    </article>

    <aside class="card editor-side">
      <h2>{{ isEdit ? '当前字数' : '写作节奏' }}</h2>
      <p class="sub">{{ contentLength }} 字</p>
      <hr class="split-line" />
      <p class="sub">想到什么就写什么。</p>
      <p class="sub">写完再决定要不要改。</p>
    </aside>
  </section>
</template>
