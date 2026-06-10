import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

// TODO(backend): AiMcpController not registered yet — endpoints will return 500 NoResourceFoundException
// Wire to backend once controller is added under /api/v1/ai/mcp
export interface McpVO {
  id: number
  projectId?: number
  name: string
  url: string
  type?: string
  apiKey?: string
  status: number
  description?: string
  createTime?: string
}

export interface McpSave {
  id?: number
  projectId?: number
  name: string
  url: string
  type?: string
  apiKey?: string
  status?: number
  description?: string
}

export const mcpApi = {
  page: (q: PageQuery) => get<PageResult<McpVO>>('/v1/ai/mcp/page', q),
  list: () => get<McpVO[]>('/v1/ai/mcp/list'),
  get: (id: number) => get<McpVO>(`/v1/ai/mcp/${id}`),
  create: (data: McpSave) => post<number>('/v1/ai/mcp', data),
  update: (data: McpSave) => put('/v1/ai/mcp', data),
  remove: (id: number) => del(`/v1/ai/mcp/${id}`)
}
