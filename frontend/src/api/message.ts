import request from '../utils/request'
import type {
  CreateSessionPayload,
  PagedResult,
  PrivateMessageItem,
  PrivateSessionItem,
  SendPrivateMessagePayload
} from '../types'

export function createOrGetSession(payload: CreateSessionPayload) {
  return request.post<unknown, PrivateSessionItem>('/messages/sessions', payload)
}

export function fetchPrivateSessions(page = 1, size = 10) {
  return request.get<unknown, PagedResult<PrivateSessionItem>>('/messages/sessions', { params: { page, size } })
}

export function fetchPrivateMessages(sessionId: number, page = 1, size = 20) {
  return request.get<unknown, PagedResult<PrivateMessageItem>>(`/messages/sessions/${sessionId}/items`, {
    params: { page, size }
  })
}

export function sendPrivateMessage(sessionId: number, payload: SendPrivateMessagePayload) {
  return request.post<unknown, PrivateMessageItem>(`/messages/sessions/${sessionId}/items`, payload)
}
