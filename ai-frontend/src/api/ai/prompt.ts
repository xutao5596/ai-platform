import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'

export interface PromptVO {
  id: number
  projectId: number
  name: string
  code: string
  description?: string
  currentVersionId?: number
  versionCount: number
  status: number
  createTime?: string
}

export interface PromptVersionVO {
  id: number
  promptId: number
  projectId: number
  version: number
  content: string
  variables?: string
  modelId?: number
  temperature?: number
  maxTokens?: number
  changelog?: string
  isActive: number
  createTime?: string
}

export interface PromptSave {
  id?: number
  projectId: number
  name: string
  code: string
  description?: string
  status?: number
}

export interface PromptVersionSave {
  id?: number
  promptId: number
  content: string
  variables?: string
  modelId?: number
  temperature?: number
  maxTokens?: number
  changelog?: string
  activate?: boolean
}

export const promptApi = {
  page: (q: PageQuery) => get<PageResult<PromptVO>>('/v1/ai/prompt/page', q),
  list: (projectId: number) => get<PromptVO[]>('/v1/ai/prompt/list', { projectId }),
  get: (id: number) => get<PromptVO>(`/v1/ai/prompt/${id}`),
  create: (data: PromptSave) => post<number>('/v1/ai/prompt', data),
  update: (data: PromptSave) => put('/v1/ai/prompt', data),
  remove: (id: number) => del(`/v1/ai/prompt/${id}`),
  versions: (id: number) => get<PromptVersionVO[]>(`/v1/ai/prompt/${id}/versions`),
  active: (id: number) => get<PromptVersionVO>(`/v1/ai/prompt/${id}/active`),
  createVersion: (data: PromptVersionSave) => post<number>('/v1/ai/prompt/version', data),
  activateVersion: (promptId: number, versionId: number) =>
    post(`/v1/ai/prompt/${promptId}/activate/${versionId}`)
}
