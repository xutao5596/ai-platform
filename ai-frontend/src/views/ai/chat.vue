<template>
  <div class="chat-page">
    <el-container class="chat-container">
      <el-aside :width="showSessions ? '240px' : '0px'" class="chat-aside">
        <div class="session-list">
          <el-button type="primary" :icon="Plus" class="new-session" @click="onNewSession">{{ t('ai.chat.newSession') }}</el-button>
          <div
            v-for="s in sessions"
            :key="s.id"
            class="session-item"
            :class="{ active: currentSessionId === s.id }"
            @click="selectSession(s)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="title">{{ s.title || t('ai.chat.defaultSessionTitle') }}</span>
            <el-icon class="del" @click.stop="onDeleteSession(s)"><Delete /></el-icon>
          </div>
        </div>
      </el-aside>
      <el-container>
        <el-header class="chat-header">
          <el-button text @click="showSessions = !showSessions">
            <el-icon><Expand v-if="!showSessions" /><Fold v-else /></el-icon>
          </el-button>
          <span class="header-title">{{ t('nav.aiChat') }}</span>
          <el-select v-model="currentModelId" :placeholder="t('ai.chat.selectModel')" style="width: 240px" @change="onModelChange">
            <el-option v-for="m in models" :key="m.id" :value="m.id" :label="`${m.name} (${m.provider})`" />
          </el-select>
        </el-header>
        <el-main class="chat-main">
          <div v-if="messages.length === 0 && !sending" class="empty">
            <el-icon size="64" class="empty-icon"><ChatDotRound /></el-icon>
            <p>{{ t('ai.chat.empty') }}</p>
            <p class="hint">{{ t('ai.chat.emptyHint') }}</p>
          </div>
          <div v-else class="messages">
            <div
              v-for="(m, idx) in messages"
              :key="idx"
              class="message"
              :class="m.role"
            >
              <div class="avatar">
                <el-avatar v-if="m.role === 'user'" :size="32">U</el-avatar>
                <el-avatar v-else :size="32" type="primary">AI</el-avatar>
              </div>
              <div class="content">
                <div v-if="m.role === 'user'" class="content-text">{{ m.content }}</div>
                <div v-else class="markdown-body" v-html="renderMarkdown(m.content || '')"></div>
                <div v-if="m.inputTokens" class="meta">
                  tokens: {{ m.inputTokens + (m.outputTokens || 0) }} | {{ m.costMs }}ms
                </div>
              </div>
            </div>
            <div v-if="sending" class="message assistant streaming">
              <div class="avatar"><el-avatar :size="32" type="primary">AI</el-avatar></div>
              <div class="content">
                <div class="markdown-body" v-html="renderMarkdown(streamingContent)"></div>
              </div>
            </div>
            <div v-if="canRegenerate" class="regen-row">
              <el-button size="small" :icon="Refresh" @click="onRegenerate">
                {{ t('ai.chat.regenerate') }}
              </el-button>
            </div>
          </div>
        </el-main>
        <el-footer class="chat-footer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            :placeholder="t('ai.chat.inputPlaceholder')"
            @keydown.enter.exact.prevent="onSend"
            :disabled="!currentModelId"
          />
          <el-button
            v-if="sending"
            type="danger"
            :icon="CircleClose"
            @click="onStop"
          >
            {{ t('ai.chat.stop') }}
          </el-button>
          <el-button
            v-else
            type="primary"
            :loading="sending"
            :disabled="!currentModelId || !input.trim()"
            @click="onSend"
          >
            {{ t('common.send') }}
          </el-button>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, ChatDotRound, Delete, Expand, Fold, CircleClose, Refresh } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/github.css'
import { chatApi, type ChatSessionVO, type ChatMessageVO, type ChatResponse } from '@/api/ai/chat'
import { modelApi, type ModelVO } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'

function getAuthToken(): string {
  try {
    const raw = localStorage.getItem('ai-platform-user')
    if (raw) {
      const obj = JSON.parse(raw)
      if (obj && typeof obj.token === 'string') return obj.token
    }
  } catch {
    // ignore
  }
  try {
    const u = useUserStore()
    if (u && u.token) return u.token
  } catch {
    // ignore
  }
  return ''
}

const { t } = useI18n()

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch {
        // fallthrough
      }
    }
    return `<pre class="hljs"><code>${escapeHtml(str)}</code></pre>`
  }
})

function renderMarkdown(text: string): string {
  if (!text) return ''
  return md.render(text)
}

const sessions = ref<ChatSessionVO[]>([])
const messages = ref<ChatMessageVO[]>([])
const currentSessionId = ref<number | null>(null)
const showSessions = ref(true)
const models = ref<ModelVO[]>([])
const currentModelId = ref<number | null>(0) as any
const input = ref('')
const sending = ref(false)
const streamingContent = ref('')
let streamHandle: { close: () => void } | null = null
let pendingUserContent: string | null = null

const canRegenerate = computed(() => {
  if (sending.value) return false
  if (messages.value.length < 2) return false
  const last = messages.value[messages.value.length - 1]
  const prev = messages.value[messages.value.length - 2]
  return last?.role === 'assistant' && prev?.role === 'user'
})

async function loadSessions() {
  sessions.value = await chatApi.sessions()
  if (sessions.value.length > 0 && !currentSessionId.value) {
    await selectSession(sessions.value[0])
  }
}

async function loadModels() {
  models.value = await modelApi.list()
  if (models.value.length > 0 && !currentModelId.value) {
    const def = models.value.find(m => m.isDefault === 1) || models.value[0]
    currentModelId.value = def.id
  }
}

async function selectSession(s: ChatSessionVO) {
  currentSessionId.value = s.id
  messages.value = await chatApi.messages(s.id)
  if (s.modelId) currentModelId.value = s.modelId
  await nextTick()
  scrollToBottom()
}

function onNewSession() {
  currentSessionId.value = null
  messages.value = []
}

async function onDeleteSession(s: ChatSessionVO) {
  await ElMessageBox.confirm(t('ai.chat.deleteConfirm', { title: s.title || s.id }), t('common.confirm'), { type: 'warning' })
  await chatApi.removeSession(s.id)
  ElMessage.success(t('ai.chat.removed'))
  if (currentSessionId.value === s.id) {
    currentSessionId.value = null
    messages.value = []
  }
  await loadSessions()
}

function onModelChange() {
  // could persist to session
}

function scrollToBottom() {
  const el = document.querySelector('.chat-main')
  if (el) el.scrollTop = el.scrollHeight
}

async function streamChat(text: string) {
  streamingContent.value = ''
  let resolveDone: (() => void) | null = null
  const donePromise = new Promise<void>(resolve => { resolveDone = resolve })
  let resolved = false
  const finish = () => {
    if (resolved) return
    resolved = true
    if (resolveDone) resolveDone()
  }

  try {
    const resp = await fetch(`/api${chatApi.streamUrl(currentSessionId.value || undefined)}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {})
      },
      body: JSON.stringify({
        modelId: currentModelId.value,
        message: text,
        sessionId: currentSessionId.value || undefined
      })
    })
    if (!resp.ok || !resp.body) {
      const errText = await resp.text().catch(() => '')
      throw new Error(`HTTP ${resp.status} ${errText}`)
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    const controller = { closed: false, close: () => { controller.closed = true; reader.cancel().catch(() => {}) } }
    streamHandle = controller

    while (!controller.closed) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx: number
      while ((idx = buffer.indexOf('\n\n')) !== -1) {
        const block = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const parsed = parseSseBlock(block)
        if (!parsed) continue
        const { event, data } = parsed
        if (event === 'content' || event === 'message') {
          if (data && typeof data.chunk === 'string') {
            streamingContent.value += data.chunk
            scrollToBottom()
          }
          if (!currentSessionId.value && data?.sessionId) {
            currentSessionId.value = data.sessionId
          }
        } else if (event === 'done') {
          if (!currentSessionId.value && data?.sessionId) {
            currentSessionId.value = data.sessionId
          }
          if (!resolved) {
            messages.value.push({
              id: data?.messageId || 0,
              sessionId: data?.sessionId || currentSessionId.value || 0,
              role: 'assistant',
              content: streamingContent.value,
              inputTokens: data?.inputTokens,
              outputTokens: data?.outputTokens,
              costMs: data?.costMs,
              status: 1
            })
            await loadSessions()
          }
          controller.close()
          finish()
          break
        } else if (event === 'error') {
          ElMessage.error(data?.message || t('ai.chat.sendFailed'))
          controller.close()
          finish()
          break
        }
      }
    }
    if (buffer.trim() && !resolved) {
      const parsed = parseSseBlock(buffer)
      if (parsed?.event === 'content' && parsed.data?.chunk) {
        streamingContent.value += parsed.data.chunk
      }
    }
  } catch (e: any) {
    if (e?.name !== 'AbortError' && e?.message !== 'aborted') {
      ElMessage.error(`${t('ai.chat.sendFailed')}: ${e?.message || e}`)
    }
  } finally {
    streamHandle = null
    finish()
  }
  return donePromise
}

function parseSseBlock(block: string): { event: string; data: any } | null {
  if (!block.trim()) return null
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
  return { event: eventName, data: payload }
}

async function onSend() {
  if (!input.value.trim() || !currentModelId.value) return
  const text = input.value
  input.value = ''
  sending.value = true
  streamingContent.value = ''
  pendingUserContent = text

  const userMsg: ChatMessageVO = {
    id: 0, sessionId: currentSessionId.value || 0, role: 'user', content: text, status: 1
  }
  messages.value.push(userMsg)
  await nextTick()
  scrollToBottom()

  try {
    await streamChat(text)
  } finally {
    sending.value = false
    streamingContent.value = ''
    pendingUserContent = null
    await nextTick()
    scrollToBottom()
  }
}

function onStop() {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
  sending.value = false
  ElMessage.info(t('ai.chat.stopped'))
}

async function onRegenerate() {
  if (!canRegenerate.value) return
  const lastUser = messages.value[messages.value.length - 2]
  if (!lastUser || lastUser.role !== 'user') return
  const text = lastUser.content || ''
  messages.value = messages.value.slice(0, -2)
  sending.value = true
  streamingContent.value = ''
  pendingUserContent = text
  messages.value.push({
    id: 0, sessionId: currentSessionId.value || 0, role: 'user', content: text, status: 1
  })
  await nextTick()
  scrollToBottom()
  try {
    await streamChat(text)
  } finally {
    sending.value = false
    streamingContent.value = ''
    pendingUserContent = null
    await nextTick()
    scrollToBottom()
  }
}

onMounted(async () => {
  await loadModels()
  await loadSessions()
})

onBeforeUnmount(() => {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
})
</script>

<style scoped>
.chat-page { padding: 0; }
.chat-container { height: calc(100vh - 56px - 32px); }
.chat-aside { background: var(--ai-chat-aside-bg); border-right: 1px solid var(--ai-border); transition: width 0.2s; overflow: hidden; }
.session-list { padding: 12px; }
.new-session { width: 100%; margin-bottom: 12px; }
.session-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 4px;
  cursor: pointer; margin-bottom: 4px;
  font-size: 14px;
  color: var(--ai-text);
}
.session-item:hover { background: var(--ai-bg-hover); }
.session-item.active { background: var(--ai-primary); color: var(--ai-text-on-primary); }
.session-item .title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-item .del { opacity: 0.5; }
.session-item .del:hover { opacity: 1; }
.chat-header {
  display: flex; align-items: center; gap: 12px;
  background: var(--ai-bg-elevated); border-bottom: 1px solid var(--ai-border);
  padding: 0 16px; height: 56px;
  color: var(--ai-text);
}
.header-title { font-weight: 600; flex: 1; }
.chat-main {
  background: var(--ai-chat-main-bg); padding: 16px 24px;
  overflow-y: auto;
}
.empty { text-align: center; padding-top: 80px; color: var(--ai-text-secondary); }
.empty p { margin: 8px 0; }
.empty .hint { font-size: 12px; color: var(--ai-text-secondary); }
.empty-icon { color: var(--ai-text-placeholder); }
.messages { display: flex; flex-direction: column; gap: 16px; max-width: 900px; margin: 0 auto; }
.message { display: flex; gap: 12px; }
.message.user { flex-direction: row-reverse; }
.message .avatar { flex-shrink: 0; }
.message .content {
  background: var(--ai-chat-bubble-bg); padding: 12px 16px; border-radius: 8px;
  color: var(--ai-text);
  box-shadow: var(--ai-shadow-sm);
  max-width: 75%;
}
.message.user .content { background: var(--ai-chat-bubble-user-bg); color: var(--ai-chat-bubble-user-text); }
.message .content .content-text {
  white-space: pre-wrap; word-wrap: break-word;
}
.message .meta { font-size: 11px; opacity: 0.6; margin-top: 6px; }
.message.streaming .content { border: 1px solid var(--ai-primary); }

.regen-row { display: flex; justify-content: flex-start; padding-left: 44px; }

.chat-footer {
  display: flex; gap: 8px; align-items: flex-end;
  background: var(--ai-bg-elevated); border-top: 1px solid var(--ai-border);
  padding: 12px 16px;
}
.chat-footer .el-textarea { flex: 1; }
</style>

<style>
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  color: inherit;
}
.markdown-body p { margin: 0 0 8px 0; }
.markdown-body p:last-child { margin-bottom: 0; }
.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin: 12px 0 8px 0; font-weight: 600;
}
.markdown-body h1 { font-size: 20px; }
.markdown-body h2 { font-size: 18px; }
.markdown-body h3 { font-size: 16px; }
.markdown-body h4 { font-size: 14px; }
.markdown-body ul, .markdown-body ol { margin: 4px 0 8px 0; padding-left: 24px; }
.markdown-body li { margin: 2px 0; }
.markdown-body code {
  background: rgba(27, 31, 35, 0.08);
  border-radius: 4px;
  padding: 1px 5px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
}
.markdown-body pre {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}
.markdown-body pre code {
  background: transparent;
  padding: 0;
  font-size: 13px;
  line-height: 1.5;
}
.markdown-body blockquote {
  border-left: 3px solid #d0d7de;
  margin: 8px 0;
  padding: 0 12px;
  color: #57606a;
}
.markdown-body table {
  border-collapse: collapse;
  margin: 8px 0;
}
.markdown-body table th, .markdown-body table td {
  border: 1px solid #d0d7de;
  padding: 6px 12px;
}
.markdown-body a { color: #0969da; text-decoration: none; }
.markdown-body a:hover { text-decoration: underline; }
.markdown-body img { max-width: 100%; }
</style>
