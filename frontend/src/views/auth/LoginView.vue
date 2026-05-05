<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const email = ref('')
const password = ref('')
const mode = ref<'login' | 'register'>('login')
const error = ref('')

const loginBackgrounds = [
  'https://picsum.photos/id/1018/2200/1400',
  'https://picsum.photos/id/1015/2200/1400',
  'https://picsum.photos/id/1011/2200/1400',
  'https://picsum.photos/id/1003/2200/1400'
]

const loginPosters = [
  'https://picsum.photos/id/1025/1300/1800',
  'https://picsum.photos/id/1043/1300/1800',
  'https://picsum.photos/id/1068/1300/1800',
  'https://picsum.photos/id/1039/1300/1800'
]

const loginBackground = loginBackgrounds[Math.floor(Math.random() * loginBackgrounds.length)]
const loginPoster = loginPosters[Math.floor(Math.random() * loginPosters.length)]

function switchMode(next: 'login' | 'register') {
  mode.value = next
  error.value = ''
}

async function submit() {
  error.value = ''
  if (!username.value || !password.value) {
    error.value = '用户名和密码必填'
    return
  }

  try {
    if (mode.value === 'login') {
      await authStore.signIn({ username: username.value, password: password.value })
      const redirect = (route.query.redirect as string) || '/notes'
      await router.push(redirect)
      return
    }

    if (!email.value) {
      error.value = '注册时邮箱必填'
      return
    }

    await authStore.signUp({ username: username.value, email: email.value, password: password.value })
    await authStore.signOut()
    switchMode('login')
    password.value = ''
    error.value = '注册成功，请登录'
  } catch (e) {
    error.value = (e as Error).message
  }
}
</script>

<template>
  <section class="auth-scene" :style="{ '--login-bg': `url(${loginBackground})` }">
    <div class="auth-shell" :class="{ 'register-mode': mode === 'register' }">
      <article class="auth-poster" :style="{ '--login-poster': `url(${loginPoster})` }">
        <div class="auth-poster-overlay">
          <p>今天也写一点。</p>
          <h3>从登录开始，继续写博客。</h3>
        </div>
      </article>

      <article class="card auth-form auth-form-centered">
        <h2 class="auth-form-title">{{ mode === 'login' ? '登录' : '注册' }}</h2>

        <p class="auth-switch-tip">
          <span>{{ mode === 'login' ? '没有账号？' : '已有账号？' }}</span>
          <button
            class="auth-text-btn"
            type="button"
            @click="switchMode(mode === 'login' ? 'register' : 'login')"
          >
            {{ mode === 'login' ? '立即注册' : '立即登录' }}
          </button>
        </p>

        <form class="form" @submit.prevent="submit">
          <label class="field">
            <span>用户名/邮箱</span>
            <input v-model.trim="username" placeholder="输入用户名或邮箱" />
          </label>

          <label v-if="mode === 'register'" class="field">
            <span>邮箱</span>
            <input v-model.trim="email" placeholder="输入邮箱" />
          </label>

          <label class="field">
            <span>登录密码</span>
            <input v-model="password" type="password" placeholder="输入密码" />
          </label>

          <div class="auth-assist">
            <span>忘记密码</span>
            <span>免密登录</span>
          </div>

          <p class="error auth-error" role="status" aria-live="polite">{{ error || ' ' }}</p>

          <button class="primary-btn" :disabled="authStore.loading" type="submit">
            {{ authStore.loading ? '处理中...' : mode === 'login' ? '登录' : '注册' }}
          </button>
        </form>
      </article>
    </div>
  </section>
</template>
