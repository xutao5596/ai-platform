import { get, post, put, del } from '@/utils/http'
import type { PageResult } from '@/utils/http'

export interface WebhookVO {
  id?: number
  projectId?: number
  name: string
  url: string
  secret?: string
  events?: string[]
  status?: number
  description?: string
  createTime?: string
  updateTime?: string
}

export interface WebhookLog {
  id: number
  webhookId: number
  projectId: number
  event: string
  requestUrl: string
  responseStatus: number
  responseBody?: string
  requestPayload?: string
  retryCount: number
  costMs?: number
  createTime?: string
}

export const WEBHOOK_EVENT_OPTIONS = [
  { value: 'flow.run.success', label: 'flow.run.success 流程成功' },
  { value: 'flow.run.failed', label: 'flow.run.failed 流程失败' },
  { value: 'assistant.chat.completed', label: 'assistant.chat.completed 助手完成' },
  { value: 'kb.doc.indexed', label: 'kb.doc.indexed 文档索引完成' },
  { value: 'webhook.test', label: 'webhook.test 测试事件' }
]

export const webhookApi = {
  list: (projectId: number) => get<WebhookVO[]>(`/v1/project/${projectId}/webhooks`),
  get: (projectId: number, id: number) => get<WebhookVO>(`/v1/project/${projectId}/webhooks/${id}`),
  create: (projectId: number, data: WebhookVO) => post<WebhookVO>(`/v1/project/${projectId}/webhooks`, data),
  update: (projectId: number, id: number, data: WebhookVO) => put<WebhookVO>(`/v1/project/${projectId}/webhooks/${id}`, data),
  remove: (projectId: number, id: number) => del(`/v1/project/${projectId}/webhooks/${id}`),
  resetSecret: (projectId: number, id: number) => post<WebhookVO>(`/v1/project/${projectId}/webhooks/${id}/reset-secret`, {}),
  test: (projectId: number, id: number, data: any) => post<any>(`/v1/project/${projectId}/webhooks/${id}/test`, data || {}),
  logs: (projectId: number, id: number, current = 1, size = 20) =>
    get<PageResult<WebhookLog>>(`/v1/project/${projectId}/webhooks/${id}/logs`, { current, size })
}
