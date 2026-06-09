import { get, post, put, del } from '@/utils/http'
import type { PageQuery, PageResult } from '@/utils/http'
import type {
  AssistantVO,
  AssistantSave,
  AssistantEventVO,
  AssistantEventSave,
  AssistantToolVO,
  AssistantToolTestReq,
  AssistantToolTestResult,
  AssistantSessionVO,
  AssistantMessageVO,
  AssistantChatReq,
  AssistantStreamEvent
} from '@/types/assistant'

export const assistantApi = {
  page: (q: PageQuery) => get<PageResult<AssistantVO>>('/v1/assistant/page', q),
  list: (projectId: number) => get<AssistantVO[]>('/v1/assistant/list', { projectId }),
  get: (id: number) => get<AssistantVO>(`/v1/assistant/${id}`),
  create: (data: AssistantSave) => post<number>('/v1/assistant', data),
  update: (data: AssistantSave) => put('/v1/assistant', data),
  remove: (id: number) => del(`/v1/assistant/${id}`)
}

export const eventApi = {
  list: (assistantId: number) => get<AssistantEventVO[]>(`/v1/assistant/${assistantId}/events`),
  create: (assistantId: number, data: AssistantEventSave) =>
    post<number>(`/v1/assistant/${assistantId}/events`, data),
  remove: (assistantId: number, eventId: number) =>
    del(`/v1/assistant/${assistantId}/events/${eventId}`)
}

export const toolApi = {
  list: () => get<AssistantToolVO[]>('/v1/assistant/tools'),
  test: (assistantId: number, data: AssistantToolTestReq) =>
    post<AssistantToolTestResult>(`/v1/assistant/${assistantId}/tools/test`, data)
}

export const assistantSessionApi = {
  list: (assistantId: number) =>
    get<AssistantSessionVO[]>('/v1/assistant/sessions', { assistantId }),
  messages: (sessionId: number) =>
    get<AssistantMessageVO[]>(`/v1/assistant/sessions/${sessionId}/messages`),
  remove: (sessionId: number) => del(`/v1/assistant/sessions/${sessionId}`)
}

export const chatApi = {
  streamUrl: (assistantId: number) => `/v1/assistant/${assistantId}/chat/stream`,
  send: (assistantId: number, data: AssistantChatReq) =>
    post<{ sessionId: number; messageId: number }>(
      `/v1/assistant/${assistantId}/chat/send`,
      data
    ),

  async stream(
    assistantId: number,
    data: AssistantChatReq,
    handlers: {
      onContent?: (chunk: string, sessionId?: number) => void
      onToolCall?: (name: string, args: any, callId: string) => void
      onToolResult?: (callId: string, result: any) => void
      onDone?: (info: {
        sessionId: number
        messageId: number
        inputTokens?: number
        outputTokens?: number
        costMs?: number
      }) => void
      onError?: (msg: string) => void
    },
    options?: { token?: string }
  ): Promise<{ close: () => void }> {
    const url = this.streamUrl(assistantId)
    const token = options?.token || (() => {
      try {
        const raw = localStorage.getItem('ai-platform-user')
        if (raw) {
          const obj = JSON.parse(raw)
          return obj.token as string | undefined
        }
      } catch {
        // ignore
      }
      return undefined
    })()

    const controller = new AbortController()
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(data),
      signal: controller.signal
    })

    if (!resp.ok || !resp.body) {
      const text = await resp.text().catch(() => '')
      handlers.onError?.(`HTTP ${resp.status} ${text}`)
      return { close: () => controller.abort() }
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const parseAndDispatch = (raw: string) => {
      const blocks = raw.split(/\r?\n\r?\n/)
      for (const block of blocks) {
        if (!block.trim()) continue
        let eventName = 'message'
        const dataLines: string[] = []
        for (const line of block.split(/\r?\n/)) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim())
          }
        }
        const dataStr = dataLines.join('\n')
        let payload: any = dataStr
        try {
          payload = dataStr ? JSON.parse(dataStr) : {}
        } catch {
          // keep raw
        }
        try {
          switch (eventName) {
            case 'content':
              handlers.onContent?.(payload.chunk ?? '', payload.sessionId)
              break
            case 'tool_call':
              handlers.onToolCall?.(payload.name, payload.args, payload.callId)
              break
            case 'tool_result':
              handlers.onToolResult?.(payload.callId, payload.result)
              break
            case 'done':
              handlers.onDone?.(payload)
              break
            case 'error':
              handlers.onError?.(payload.message || dataStr)
              break
          }
        } catch (err) {
          handlers.onError?.(`handler error: ${(err as Error).message}`)
        }
      }
    }

    ;(async () => {
      try {
        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })
          let idx: number
          while ((idx = buffer.indexOf('\n\n')) !== -1) {
            const block = buffer.slice(0, idx)
            buffer = buffer.slice(idx + 2)
            parseAndDispatch(block)
          }
        }
        if (buffer.trim()) parseAndDispatch(buffer)
      } catch (err: any) {
        if (err?.name !== 'AbortError') {
          handlers.onError?.(err?.message || 'stream read error')
        }
      }
    })()

    return {
      close: () => controller.abort()
    }
  }
}

export type { AssistantStreamEvent }
