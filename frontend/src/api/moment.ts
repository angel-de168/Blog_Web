import request from '../utils/request'
import type {
  MomentComment,
  MomentItem,
  MomentQuery,
  PagedResult,
  SaveMomentCommentPayload,
  SaveMomentPayload
} from '../types'

export function fetchMoments(query: MomentQuery) {
  return request.get<unknown, PagedResult<MomentItem>>('/moments', { params: query })
}

export function fetchMoment(id: number) {
  return request.get<unknown, MomentItem>(`/moments/${id}`)
}

export function saveMoment(payload: SaveMomentPayload) {
  if (payload.id) {
    return request.put<unknown, MomentItem>(`/moments/${payload.id}`, payload)
  }
  return request.post<unknown, MomentItem>('/moments', payload)
}

export function deleteMoment(id: number) {
  return request.delete(`/moments/${id}`)
}

export function likeMoment(id: number) {
  return request.post<unknown, MomentItem>(`/moments/${id}/like`)
}

export function unlikeMoment(id: number) {
  return request.delete<unknown, MomentItem>(`/moments/${id}/like`)
}

export function fetchMomentComments(id: number, page = 1, size = 10) {
  return request.get<unknown, PagedResult<MomentComment>>(`/moments/${id}/comments`, { params: { page, size } })
}

export function createMomentComment(id: number, payload: SaveMomentCommentPayload) {
  return request.post<unknown, MomentComment>(`/moments/${id}/comments`, payload)
}

export function deleteMomentComment(id: number, commentId: number) {
  return request.delete(`/moments/${id}/comments/${commentId}`)
}
