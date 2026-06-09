import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface ModelVO {
  id: number
  name: string
  provider: string
  modelName: string
  apiBase?: string
  apiKey?: string
  proxyEnabled?: number
  proxyUrl?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  embeddingModel?: string
  dimension?: number
  status: number
  isDefault: number
  description?: string
  createTime?: string
}

export interface ModelSave {
  id?: number
  name: string
  provider: string
  modelName: string
  apiBase?: string
  apiKey: string
  proxyEnabled?: number
  proxyUrl?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  embeddingModel?: string
  dimension?: number
  status?: number
  isDefault?: number
  description?: string
}

export const modelApi = {
  page: (q: PageQuery) => get<PageResult<ModelVO>>('/v1/ai/model/page', q),
  list: () => get<ModelVO[]>('/v1/ai/model/list'),
  embedding: () => get<ModelVO[]>('/v1/ai/model/embedding'),
  get: (id: number) => get<ModelVO>(`/v1/ai/model/${id}`),
  create: (data: ModelSave) => post<number>('/v1/ai/model', data),
  update: (data: ModelSave) => put('/v1/ai/model', data),
  remove: (id: number) => del(`/v1/ai/model/${id}`),
  test: (id: number) => post<{ success: boolean }>(`/v1/ai/model/${id}/test`)
}
