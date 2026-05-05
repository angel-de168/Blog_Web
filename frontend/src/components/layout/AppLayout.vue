<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const notice = ref('')
const noticeTarget = ref('/notes')
const isNavHidden = ref(false)
const isNavScrolled = ref(false)

const profileIcon = 'https://api.iconify.design/solar:user-circle-outline.svg?color=%232f645b'
const logoutIcon = 'https://api.iconify.design/solar:logout-2-outline.svg?color=%238b3f38'

const menuItems = [
  { label: '首页', path: '/', tip: '先看博客节奏', public: true },
  { label: '博客', path: '/notes', tip: '写下今天收获', public: true },
  { label: '随笔', path: '/moments', tip: '随时记一段心情', public: true },
  { label: '我的关注', path: '/following', tip: '看看你在关注谁', public: false },
  { label: '私信', path: '/messages', tip: '和朋友聊聊近况', public: false },
  { label: '成长图谱', path: '/growth', tip: '回看学习轨迹', public: true }
]

const isPublicPage = computed(() => Boolean(route.meta.public))
const isHomePage = computed(() => route.path === '/')
const isLoginPage = computed(() => route.path === '/login')
const isHomeStyledRoute = computed(() => {
  if (route.path === '/') {
    return true
  }
  if (route.path === '/notes' || route.path === '/moments' || route.path === '/growth' || route.path === '/following' || route.path === '/messages') {
    return true
  }
  return /^\/notes\/\d+$/.test(route.path)
})
const useHomeNavStyle = computed(() => isHomeStyledRoute.value || isLoginPage.value)
const isHeroTopFitPage = computed(() => {
  if (route.path === '/notes' || route.path === '/moments' || route.path === '/growth') {
    return true
  }
  return /^\/notes\/\d+$/.test(route.path)
})
const avatarLetter = computed(() => authStore.user?.username?.trim().charAt(0) || '知')

let lastScrollY = 0
let rafId = 0

function syncScrollState() {
  const currentScrollY = Math.max(0, window.scrollY || window.pageYOffset || 0)
  const delta = currentScrollY - lastScrollY

  isNavScrolled.value = currentScrollY > 8

  if (currentScrollY <= 32) {
    isNavHidden.value = false
  } else if (delta > 8 && currentScrollY > 72) {
    isNavHidden.value = true
  } else if (delta < -8) {
    isNavHidden.value = false
  }

  lastScrollY = currentScrollY
  rafId = 0
}

function handleScroll() {
  if (rafId) {
    return
  }
  rafId = window.requestAnimationFrame(syncScrollState)
}

function resetNavState() {
  isNavHidden.value = false
  isNavScrolled.value = false
  lastScrollY = Math.max(0, window.scrollY || window.pageYOffset || 0)
}

function clearNotice() {
  notice.value = ''
}

async function handleMenuClick(item: (typeof menuItems)[number]) {
  clearNotice()
  if (!item.public && !authStore.user) {
    await goLogin(item.path)
    return
  }
  await router.push(item.path)
}

async function goLogin(path?: string) {
  clearNotice()
  const redirect = path || route.fullPath
  await router.push({ name: 'login', query: { redirect } })
}

async function goProfile() {
  await router.push('/notes')
}

async function handleLogout() {
  await authStore.signOut()
  await router.push('/')
}

onMounted(() => {
  resetNavState()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  if (rafId) {
    window.cancelAnimationFrame(rafId)
    rafId = 0
  }
})

watch(
  () => route.fullPath,
  () => {
    resetNavState()
  }
)
</script>

<template>
  <div
    class="layout"
    :class="{
      public: isPublicPage,
      'is-home': isHomePage,
      'is-home-fixed': isHomePage,
      'nav-home-style': useHomeNavStyle,
      'hero-top-fit': isHeroTopFitPage,
      'is-login': isLoginPage,
      'is-nav-hidden': isNavHidden,
      'is-nav-scrolled': isNavScrolled
    }"
  >
    <header class="topbar">
      <div class="brand-box">
        <p class="brand-kicker">Blog Diary</p>
        <div class="brand">知行博客</div>
      </div>

      <nav class="menu">
        <button
          v-for="item in menuItems"
          :key="item.path"
          class="menu-link"
          :class="{ active: route.path === item.path }"
          type="button"
          @click="handleMenuClick(item)"
        >
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="actions">
        <template v-if="useHomeNavStyle">
          <div v-if="authStore.user" class="home-user-menu">
            <button class="home-avatar-btn" type="button" aria-label="用户菜单">
              <span class="home-avatar-face">{{ avatarLetter }}</span>
            </button>

            <section class="home-user-popover">
              <div class="home-user-top">
                <div class="home-pop-avatar">{{ avatarLetter }}</div>
                <p class="home-pop-name">{{ authStore.user.username }}</p>
              </div>
              <div class="home-pop-actions">
                <button class="home-pop-link" type="button" @click="goProfile">
                  <img class="home-pop-icon" :src="profileIcon" alt="" aria-hidden="true" />
                  <span>个人中心</span>
                </button>
                <button class="home-pop-link danger" type="button" @click="handleLogout">
                  <img class="home-pop-icon" :src="logoutIcon" alt="" aria-hidden="true" />
                  <span>退出</span>
                </button>
              </div>
            </section>
          </div>

          <button
            v-else
            class="primary-btn home-action-circle"
            aria-label="去登录"
            @click="goLogin('/notes')"
          >
            登录
          </button>
        </template>

        <template v-else>
          <p v-if="authStore.user" class="username">{{ authStore.user.username }}</p>
          <button v-if="authStore.user" class="ghost-btn" @click="handleLogout">退出登录</button>
          <button v-else class="primary-btn" @click="goLogin('/notes')">去登录</button>
        </template>
      </div>
    </header>

    <div v-if="notice" class="login-notice-mask" @click="clearNotice">
      <div class="login-notice" role="dialog" aria-modal="true" @click.stop>
        <p>{{ notice }}</p>
        <div class="login-notice-actions">
          <button class="primary-btn" type="button" @click="goLogin(noticeTarget)">去登录</button>
          <button class="ghost-btn" type="button" @click="clearNotice">知道了</button>
        </div>
      </div>
    </div>

    <div class="workspace">
      <aside v-if="!isPublicPage" class="side-rail">
        <p class="rail-title">今天的小目标</p>
        <ul>
          <li>写 1 条笔记</li>
          <li>发 1 条随笔</li>
          <li>回看 1 次图谱</li>
        </ul>
      </aside>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
