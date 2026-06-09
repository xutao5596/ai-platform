import { get, post, del } from '@/utils/http'

export interface ChatSessionVO {
  id: number
  projectId?: number
  userId: number
  title?: string
  modelId?: number
  systemPrompt?: string
  temperature?: number
  maxTokens?: number
  kbIds?: string
  messageCount: number
  tokenUsed: number
  status: number
  pin: number
  createTime?: string
  updateTime?: string
}

export interface ChatMessageVO {
  id: number
  sessionId: number
  role: string
  content?: string
  reasoning?: string
  toolCalls?: string
  toolCallId?: string
  name?: string
  inputTokens?: number
  outputTokens?: number
  costMs?: number
  status?: number
  errorMsg?: string
  createTime?: string
}

export interface ChatRequest {
  sessionId?: number
  title?: string
  projectId?: number
  modelId: number
  systemPrompt?: string
  temperature?: number
  maxTokens?: number
  kbIds?: string
  message: string
}

export interface ChatResponse {
  sessionId: number
  messageId: number
  content: string
  reasoning?: string
  inputTokens?: number
  outputTokens?: number
  costMs?: number
}

export const chatApi = {
  sessions: () => get<ChatSessionVO[]>('/v1/ai/chat/sessions'),
  session: (id: number) => get<ChatSessionVO>(`/v1/ai/chat/session/${id}`),
  messages: (id: number) => get<ChatMessageVO[]>(`/v1/ai/chat/session/${id}/messages`),
  send: (data: ChatRequest) => post<ChatResponse>('/v1/ai/chat/send', data),
  removeSession: (id: number) => del(`/v1/ai/chat/session/${id}`),
  streamUrl: (sessionId?: number) =>
    sessionId ? `/v1/ai/chat/stream?sessionId=${sessionId}` : '/v1/ai/chat/stream'
}
