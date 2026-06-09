import { get, post, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface LogVO {
  id: number
  module?: string
  action?: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  userId?: number
  username?: string
  ip?: string
  costMs?: number
  status?: number
  errorMsg?: string
  createTime?: string
}

export const logApi = {
  page: (q: PageQuery) => get<PageResult<LogVO>>('/v1/system/log/page', q),
  remove: (ids: number[]) => del('/v1/system/log', { data: ids }),
  clear: () => del('/v1/system/log/clear')
}
