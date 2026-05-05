import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/home/HomeView.vue'),
      meta: { public: true }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/auth/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/notes',
      name: 'notes',
      component: () => import('../views/note/NoteListView.vue'),
      meta: { public: true }
    },
    {
      path: '/notes/new',
      name: 'note-new',
      component: () => import('../views/note/NoteEditorView.vue')
    },
    {
      path: '/notes/:id/edit',
      name: 'note-edit',
      component: () => import('../views/note/NoteEditorView.vue')
    },
    {
      path: '/notes/:id',
      name: 'note-detail',
      component: () => import('../views/note/NoteDetailView.vue'),
      meta: { public: true }
    },
    {
      path: '/moments',
      name: 'moments',
      component: () => import('../views/moment/MomentListView.vue'),
      meta: { public: true }
    },
    {
      path: '/moments/new',
      name: 'moment-new',
      component: () => import('../views/moment/MomentEditorView.vue')
    },
    {
      path: '/moments/:id/edit',
      name: 'moment-edit',
      component: () => import('../views/moment/MomentEditorView.vue')
    },
    {
      path: '/following',
      name: 'following',
      component: () => import('../views/follow/FollowingListView.vue')
    },
    {
      path: '/followers',
      name: 'followers',
      component: () => import('../views/follow/FollowerListView.vue')
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('../views/message/MessageSessionListView.vue')
    },
    {
      path: '/messages/:id',
      name: 'message-detail',
      component: () => import('../views/message/MessageSessionDetailView.vue')
    },
    {
      path: '/growth',
      name: 'growth',
      component: () => import('../views/growth/GrowthDashboardView.vue'),
      meta: { public: true }
    }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.checked) {
    await authStore.fetchCurrentUser()
  }

  if (to.meta.public) {
    return true
  }

  if (!authStore.user) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
