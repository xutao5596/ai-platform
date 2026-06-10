import { get, post, put, del } from '@/utils/http'

export interface ApiKeyVO {
  id: number
  name: string
  apiKey?: string
  apiSecret?: string
  maskedSecret?: string
  scopes?: string[]
  rateLimit?: number
  expiresAt?: number
  status: number
  lastUsedTime?: string
  lastUsedIp?: string
  createdAt?: string
}

export interface ApiKeySave {
  id?: number
  name: string
  scopes?: string[]
  rateLimit?: number
  expiresAt?: number
  status?: number
}

export const apiKeyApi = {
  list: (projectId: number) => get<ApiKeyVO[]>(`/v1/project/${projectId}/apikeys`),
  create: (projectId: number, data: ApiKeySave) =>
    post<ApiKeyVO>(`/v1/project/${projectId}/apikeys`, data),
  update: (projectId: number, id: number, data: ApiKeySave) =>
    put<ApiKeyVO>(`/v1/project/${projectId}/apikeys/${id}`, data),
  remove: (projectId: number, id: number) => del(`/v1/project/${projectId}/apikeys/${id}`),
  reset: (projectId: number, id: number) =>
    post<ApiKeyVO>(`/v1/project/${projectId}/apikeys/${id}/reset`)
}
