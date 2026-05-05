export interface UserProfile {
  id: number
  username: string
  email: string
  avatar?: string | null
  bio?: string | null
}

export interface SiteStats {
  noteCount: number
  tagCount: number
  visitCount: number
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

export type LearningStatus = 'LEARNING' | 'MASTERED'

export interface NoteItem {
  id: number
  title: string
  content: string
  categoryId?: number | null
  categoryName?: string | null
  status: LearningStatus
  tags: string[]
  coverImage?: string | null
  readCount?: number
  commentCount?: number
  authorName?: string
  createdAt: string
  updatedAt: string
}

export interface NoteQuery {
  keyword?: string
  status?: LearningStatus | ''
  contentKeyword?: string
  startTime?: string
  endTime?: string
  minReadCount?: number
  maxReadCount?: number
  sortBy?: 'updatedAt' | 'contentLength' | 'readCount'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

export interface NoteComment {
  id: number
  userId: number
  username: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface SaveNoteCommentPayload {
  content: string
}

export type MomentVisibilityScope = 'PUBLIC' | 'PRIVATE_SELF' | 'CIRCLE'

export interface MomentItem {
  id: number
  content: string
  mood: string
  visibilityScope: MomentVisibilityScope
  likeCount: number
  commentCount: number
  likedByCurrentUser: boolean
  createdAt: string
  updatedAt: string
}

export interface MomentQuery {
  keyword?: string
  mood?: string
  contentKeyword?: string
  visibilityScope?: MomentVisibilityScope
  startTime?: string
  endTime?: string
  minLikeCount?: number
  maxLikeCount?: number
  sortBy?: 'updatedAt' | 'likeCount' | 'commentCount' | 'contentLength'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}

export interface SaveMomentPayload {
  id?: number
  content: string
  mood?: string
  visibilityScope?: MomentVisibilityScope
}

export interface MomentComment {
  id: number
  userId: number
  username: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface SaveMomentCommentPayload {
  content: string
}

export interface FollowUserItem {
  id: number
  username: string
  avatar?: string | null
  bio?: string | null
}

export interface FollowStatusResult {
  followed: boolean
}

export interface PrivateSessionItem {
  id: number
  peerUserId: number
  peerUsername: string
  peerAvatar?: string | null
  peerBio?: string | null
  lastMessage?: string | null
  lastMessageAt?: string | null
}

export interface PrivateMessageItem {
  id: number
  sessionId: number
  senderUserId: number
  senderUsername: string
  senderAvatar?: string | null
  content: string
  createdAt: string
}

export interface CreateSessionPayload {
  targetUserId: number
}

export interface SendPrivateMessagePayload {
  content: string
}
export interface PagedResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface TimelinePoint {
  date: string
  count: number
}

export interface SkillPoint {
  tag: string
  value: number
}

export interface WeekdayPoint {
  label: string
  count: number
}

export interface StatusPoint {
  status: 'LEARNING' | 'MASTERED'
  count: number
}

export interface GrowthSummary {
  totalNotes: number
  activeDays: number
  peakDayCount: number
  avgPerWeek: number
}

export interface LoopSummary {
  notesCreated: number
  momentsCreated: number
  totalOutput: number
  noteCommentsReceived: number
  momentCommentsReceived: number
  momentLikesReceived: number
  totalFeedback: number
  notesIterated: number
  iterationCoverage: number
  avgIterationLagDays: number
  feedbackPerOutput: number
}

export interface LoopTimelinePoint {
  date: string
  outputCount: number
  feedbackCount: number
  iterationCount: number
}

export interface GrowthOverview {
  timeline: TimelinePoint[]
  skills: SkillPoint[]
  weekday: WeekdayPoint[]
  statuses: StatusPoint[]
  summary: GrowthSummary
  loopSummary: LoopSummary
  loopTimeline: LoopTimelinePoint[]
}
