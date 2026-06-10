import { get, post, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface LogVO {
  id: number
  module?: string
  action?: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  requestParams?: string
  responseData?: string
  userId?: number
  username?: string
  ip?: string
  userAgent?: string
  costMs?: number
  status?: number
  errorMsg?: string
  createTime?: string
}

export interface LogQuery extends PageQuery {
  module?: string
  action?: string
  username?: string
  status?: number
  startTime?: string
  endTime?: string
}

export const logApi = {
  page: (q: LogQuery) => get<PageResult<LogVO>>('/v1/system/log/page', q),
  detail: (id: number) => get<LogVO>(`/v1/system/log/${id}`),
  remove: (ids: number[]) => del('/v1/system/log', { data: ids }),
  clear: () => del('/v1/system/log/clear')
}
