import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface KnowledgeVO {
  id: number
  projectId: number
  name: string
  description?: string
  modelId?: number
  chunkSize?: number
  chunkOverlap?: number
  sep?: string
  docCount: number
  chunkCount: number
  status: number
  createTime?: string
}

export interface KnowledgeSave {
  id?: number
  projectId: number
  name: string
  description?: string
  modelId?: number
  chunkSize?: number
  chunkOverlap?: number
  sep?: string
  status?: number
}

export const knowledgeApi = {
  page: (q: PageQuery) => get<PageResult<KnowledgeVO>>('/v1/ai/knowledge/page', q),
  list: (projectId?: number) => get<KnowledgeVO[]>('/v1/ai/knowledge/list', { projectId }),
  get: (id: number) => get<KnowledgeVO>(`/v1/ai/knowledge/${id}`),
  create: (data: KnowledgeSave) => post<number>('/v1/ai/knowledge', data),
  update: (data: KnowledgeSave) => put('/v1/ai/knowledge', data),
  remove: (id: number) => del(`/v1/ai/knowledge/${id}`)
}
