<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchNote, saveNote } from '../../api/note'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')

const form = reactive({
  id: undefined as number | undefined,
  title: '',
  content: '',
  status: 'LEARNING' as 'LEARNING' | 'MASTERED',
  tagsText: '',
  categoryId: null as number | null
})

const isEdit = computed(() => Boolean(route.params.id))
const contentLength = computed(() => form.content.trim().length)
const tagCount = computed(() => form.tagsText.split(',').map((x) => x.trim()).filter(Boolean).length)

async function loadDetail() {
  if (!isEdit.value) {
    return
  }

  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    const note = await fetchNote(id)
    form.id = note.id
    form.title = note.title
    form.content = note.content
    form.status = note.status
    form.tagsText = note.tags.join(', ')
    form.categoryId = note.categoryId ?? null
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

async function submit() {
  error.value = ''
  if (!form.title || !form.content) {
    error.value = '标题和内容必填'
    return
  }

  loading.value = true
  try {
    await saveNote({
      id: form.id,
      title: form.title,
      content: form.content,
      status: form.status,
      tags: form.tagsText
        .split(',')
        .map((x) => x.trim())
        .filter(Boolean),
      categoryId: form.categoryId
    })
    await router.push({ path: '/notes', query: { saved: isEdit.value ? 'updated' : 'created' } })
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
        <h1>{{ isEdit ? '打磨这条笔记' : '写下今天这条' }}</h1>
        <p class="sub">先记要点，再补细节。</p>
      </div>

      <form class="form" @submit.prevent="submit">
        <label class="field">
          <span>标题</span>
          <input v-model.trim="form.title" placeholder="比如：MyBatis 分页踩坑" />
        </label>

        <div class="editor-inline">
          <label class="field">
            <span>学习状态</span>
            <select v-model="form.status">
              <option value="LEARNING">学习中</option>
              <option value="MASTERED">已掌握</option>
            </select>
          </label>

          <label class="field">
            <span>标签</span>
            <input v-model.trim="form.tagsText" placeholder="比如：SpringBoot, MySQL" />
          </label>
        </div>

        <label class="field">
          <span>内容</span>
          <textarea v-model="form.content" rows="15" placeholder="写清楚场景、问题、结论。" />
        </label>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="row">
          <button class="primary-btn" :disabled="loading" type="submit">{{ loading ? '保存中...' : '保存' }}</button>
          <button class="ghost-btn" type="button" @click="router.push('/notes')">先返回</button>
        </div>
      </form>
    </article>

    <aside class="card editor-side">
      <h2>{{ isEdit ? '这次改动' : '写作节奏' }}</h2>
      <p class="sub">字数 {{ contentLength }}</p>
      <p class="sub">标签 {{ tagCount }} 个</p>
      <hr class="split-line" />
      <p class="sub">先写结论，再补过程。</p>
      <p class="sub">每条只解决一个问题。</p>
    </aside>
  </section>
</template>
