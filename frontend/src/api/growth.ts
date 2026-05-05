import request from '../utils/request'
import type { GrowthOverview } from '../types'

export function fetchGrowthOverview(days = 120) {
  return request.get<unknown, GrowthOverview>('/growth/overview', {
    params: { days }
  })
}
