import request from '../utils/request'
import type { NoteComment, NoteItem, NoteQuery, PagedResult, SaveNoteCommentPayload } from '../types'

export interface SaveNotePayload {
  id?: number
  title: string
  content: string
  status: 'LEARNING' | 'MASTERED'
  tags: string[]
  categoryId?: number | null
  coverImage?: string
}

export function fetchNotes(query: NoteQuery) {
  return request.get<unknown, PagedResult<NoteItem>>('/notes', { params: query })
}

export function fetchNote(id: number) {
  return request.get<unknown, NoteItem>(`/notes/${id}`)
}

export function fetchNoteComments(id: number, page = 1, size = 10) {
  return request.get<unknown, PagedResult<NoteComment>>(`/notes/${id}/comments`, { params: { page, size } })
}

export function createNoteComment(id: number, payload: SaveNoteCommentPayload) {
  return request.post<unknown, NoteComment>(`/notes/${id}/comments`, payload)
}

export function deleteNoteComment(id: number, commentId: number) {
  return request.delete(`/notes/${id}/comments/${commentId}`)
}

export function saveNote(payload: SaveNotePayload) {
  if (payload.id) {
    return request.put<unknown, NoteItem>(`/notes/${payload.id}`, payload)
  }
  return request.post<unknown, NoteItem>('/notes', payload)
}

export function deleteNote(id: number) {
  return request.delete(`/notes/${id}`)
}
