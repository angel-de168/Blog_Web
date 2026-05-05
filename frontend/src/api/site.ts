import request from '../utils/request'
import type { SiteStats } from '../types'

export function getSiteStats() {
  return request.get<unknown, SiteStats>('/site/stats')
}
