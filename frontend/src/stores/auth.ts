import { defineStore } from 'pinia'
import { getCurrentUser, login, logout, register } from '../api/auth'
import type { LoginPayload, RegisterPayload, UserProfile } from '../types'

interface AuthState {
  user: UserProfile | null
  checked: boolean
  loading: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    checked: false,
    loading: false
  }),
  actions: {
    async fetchCurrentUser() {
      try {
        this.user = await getCurrentUser()
      } catch {
        this.user = null
      } finally {
        this.checked = true
      }
    },
    async signIn(payload: LoginPayload) {
      this.loading = true
      try {
        this.user = await login(payload)
        this.checked = true
      } finally {
        this.loading = false
      }
    },
    async signUp(payload: RegisterPayload) {
      this.loading = true
      try {
        this.user = await register(payload)
        this.checked = true
      } finally {
        this.loading = false
      }
    },
    async signOut() {
      await logout()
      this.user = null
      this.checked = true
    }
  }
})
