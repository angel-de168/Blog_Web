import request from '../utils/request'
import type { FollowUserItem, PagedResult } from '../types'

export function followUser(targetUserId: number) {
  return request.post(`/follows/${targetUserId}`)
}

export function unfollowUser(targetUserId: number) {
  return request.delete(`/follows/${targetUserId}`)
}

export function fetchFollowing(page = 1, size = 10) {
  return request.get<unknown, PagedResult<FollowUserItem>>('/follows/following', { params: { page, size } })
}

export function fetchFollowers(page = 1, size = 10) {
  return request.get<unknown, PagedResult<FollowUserItem>>('/follows/followers', { params: { page, size } })
}

export function fetchFollowStatus(targetUserId: number) {
  return request.get<unknown, boolean>('/follows/status', { params: { targetUserId } })
}
