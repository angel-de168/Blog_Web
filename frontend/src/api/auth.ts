import request from '../utils/request'
import type { LoginPayload, RegisterPayload, UserProfile } from '../types'

export function login(payload: LoginPayload) {
  return request.post<unknown, UserProfile>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return request.post<unknown, UserProfile>('/auth/register', payload)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get<unknown, UserProfile>('/auth/me')
}

export function getMainUser() {
  return request.get<unknown, UserProfile>('/auth/main-user')
}
