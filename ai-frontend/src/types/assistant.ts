export interface AssistantVO {
  id: number
  projectId: number
  name: string
  description?: string
  avatar?: string
  persona?: string
  modelId?: number
  modelName?: string
  kbIds?: number[]
  toolsEnabled?: string[]
  enableMemory?: number
  enableEventSub?: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface AssistantSave {
  id?: number
  projectId: number
  name: string
  description?: string
  avatar?: string
  persona?: string
  modelId?: number
  kbIds?: number[]
  toolsEnabled?: string[]
  enableMemory?: number
  enableEventSub?: number
  status?: number
}

export interface AssistantEventVO {
  id: number
  assistantId: number
  eventType: string
  filter?: any
  enabled: number
  description?: string
  createTime?: string
}

export interface AssistantEventSave {
  eventType: string
  filter?: any
  enabled?: number
  description?: string
}

export interface AssistantToolVO {
  name: string
  displayName: string
  description?: string
  category?: string
  params?: any
  enabled?: number
}

export interface AssistantToolTestReq {
  toolName: string
  args?: any
}

export interface AssistantToolTestResult {
  success: boolean
  result?: any
  errorMsg?: string
  costMs?: number
}

export interface AssistantSessionVO {
  id: number
  assistantId: number
  userId: number
  title?: string
  messageCount: number
  tokenUsed: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface AssistantMessageVO {
  id: number
  sessionId: number
  role: 'user' | 'assistant' | 'system' | 'tool_call' | 'tool_result'
  content?: string
  toolName?: string
  toolArgs?: any
  toolResult?: any
  toolCallId?: string
  inputTokens?: number
  outputTokens?: number
  costMs?: number
  status?: number
  createTime?: string
}

export interface AssistantChatReq {
  message: string
  sessionId?: number
  toolsEnabled?: string[]
}

export interface AssistantChatDone {
  sessionId: number
  messageId: number
  inputTokens?: number
  outputTokens?: number
  costMs?: number
}

export type AssistantStreamEvent =
  | { type: 'content'; chunk: string; sessionId?: number }
  | { type: 'tool_call'; name: string; args?: any; callId: string }
  | { type: 'tool_result'; callId: string; result: any }
  | { type: 'done'; sessionId: number; messageId: number; inputTokens?: number; outputTokens?: number; costMs?: number }
  | { type: 'error'; message: string }
