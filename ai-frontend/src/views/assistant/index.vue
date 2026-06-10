<template>
  <div class="assistant-page">
    <el-container class="as-container">
      <el-aside :width="showList ? '260px' : '0px'" class="as-aside">
        <div class="as-list">
          <el-button type="primary" :icon="Plus" class="new-btn" @click="onNew">{{ t('assistant.new') }}</el-button>
          <div
            v-for="a in assistants"
            :key="a.id"
            class="as-item"
            :class="{ active: currentId === a.id }"
            @click="select(a)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <div class="meta">
              <div class="name">{{ a.name }}</div>
              <div class="desc text-muted">{{ a.description || a.persona?.slice(0, 24) || '—' }}</div>
            </div>
          </div>
        </div>
      </el-aside>
      <el-container>
        <el-header class="as-header">
          <el-button text @click="showList = !showList">
            <el-icon><Expand v-if="!showList" /><Fold v-else /></el-icon>
          </el-button>
          <span class="title">{{ current?.name || t('assistant.selectOrCreate') }}</span>
          <div class="flex-spacer" />
          <el-button v-if="current" text @click="showConfig = !showConfig">
            <el-icon><Setting /></el-icon>
            {{ t('assistant.config') }}
          </el-button>
        </el-header>

        <el-main class="as-main">
          <div v-if="!current" class="empty">
            <el-icon size="64" class="empty-icon"><ChatDotRound /></el-icon>
            <p>{{ t('assistant.empty') }}</p>
          </div>
          <div v-else class="messages">
            <div v-for="(m, idx) in messages" :key="idx" class="msg" :class="m.role">
              <div class="avatar">
                <el-avatar v-if="m.role === 'user'" :size="32">U</el-avatar>
                <el-avatar v-else-if="m.role === 'tool'" :size="32" type="warning">T</el-avatar>
                <el-avatar v-else :size="32" type="primary">AI</el-avatar>
              </div>
              <div class="content">
                <pre v-if="m.role === 'tool'">{{ t('assistant.toolCall', { name: m.name }) }}\n{{ m.args }}</pre>
                <pre v-else-if="m.role === 'tool_result'">{{ t('assistant.toolResult', { content: m.content }) }}</pre>
                <pre v-else>{{ m.content }}</pre>
                <div v-if="m.tokens" class="meta">
                  tokens: {{ m.tokens }} | {{ m.costMs }}ms
                </div>
              </div>
            </div>
            <div v-if="streaming" class="msg assistant streaming">
              <div class="avatar"><el-avatar :size="32" type="primary">AI</el-avatar></div>
              <div class="content"><pre>{{ streamingContent || t('assistant.thinking') }}</pre></div>
            </div>
          </div>
        </el-main>

        <el-footer class="as-footer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            :placeholder="t('assistant.inputPlaceholder')"
            @keydown.enter.exact.prevent="onSend"
            :disabled="!current"
          />
          <el-button type="primary" :loading="streaming" :disabled="!current || !input.trim()" @click="onSend">
            {{ t('assistant.send') }}
          </el-button>
        </el-footer>
      </el-container>

      <el-aside v-if="showConfig && current" :width="'320px'" class="config-aside">
        <div class="config-pane">
          <h4>{{ t('assistant.configPane', { name: current.name }) }}</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item :label="t('assistant.project')">#{{ current.projectId }}</el-descriptions-item>
            <el-descriptions-item :label="t('assistant.model')">#{{ current.modelId }}</el-descriptions-item>
            <el-descriptions-item :label="t('assistant.persona')">{{ current.persona || '—' }}</el-descriptions-item>
            <el-descriptions-item :label="t('assistant.kb')">{{ (current.kbIds ? current.kbIds.join(', ') : '—') }}</el-descriptions-item>
            <el-descriptions-item :label="t('assistant.tools')">
              <el-tag v-for="tool in parseTools(current.toolsEnabled)" :key="tool" size="small" class="m-1">{{ tool }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('assistant.temperature')">{{ (current as any).temperature }}</el-descriptions-item>
            <el-descriptions-item :label="t('assistant.status')">
              <el-tag :type="current.status === 1 ? 'success' : 'info'">
                {{ current.status === 1 ? t('common.enabled') : t('common.disabled') }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <h4 class="mt">{{ t('assistant.eventSub') }}</h4>
          <el-button size="small" type="primary" plain @click="onAddEvent">{{ t('assistant.addEvent') }}</el-button>
          <div v-for="ev in events" :key="ev.id" class="event-item">
            <el-tag size="small">{{ ev.eventType }}</el-tag>
            <el-button size="small" text type="danger" @click="onRemoveEvent(ev)">×</el-button>
          </div>
        </div>
      </el-aside>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { Plus, ChatDotRound, Setting, Expand, Fold } from '@element-plus/icons-vue'
import { assistantApi, eventApi, chatApi, type AssistantStreamEvent } from '@/api/assistant'
import type { AssistantVO, AssistantEventVO } from '@/types/assistant'
import { modelApi, type ModelVO } from '@/api/ai/model'
import { useUserStore } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStore()

const assistants = ref<AssistantVO[]>([])
const current = ref<AssistantVO | null>(null)
const currentId = ref<number | null>(null)
const showList = ref(true)
const showConfig = ref(true)
const models = ref<ModelVO[]>([])

interface ChatBubble {
  role: 'user' | 'assistant' | 'tool' | 'tool_result'
  content?: string
  name?: string
  args?: any
  tokens?: number
  costMs?: number
}
const messages = ref<ChatBubble[]>([])
const input = ref('')
const streaming = ref(false)
const streamingContent = ref('')

const events = ref<AssistantEventVO[]>([])

async function load() {
  assistants.value = await assistantApi.list(1)
  models.value = await modelApi.list()
}

async function select(a: AssistantVO) {
  current.value = a
  currentId.value = a.id
  messages.value = []
  events.value = await eventApi.list(a.id)
}

function parseTools(s?: string | string[]): string[] {
  if (!s) return []
  if (Array.isArray(s)) return s
  try { return JSON.parse(s) } catch { return s.split(',') }
}

function onNew() {
  ElMessageBox.prompt(t('assistant.newDialogPlaceholder'), t('assistant.newDialogTitle'), { inputValue: t('assistant.newDefaultName') })
    .then(async ({ value }) => {
      const newId = await assistantApi.create({
        projectId: 1, name: value, persona: t('assistant.newDefaultPersona'), modelId: models.value[0]?.id || 1
      } as any)
      await load()
      const a = assistants.value.find(x => x.id === newId)
      if (a) await select(a)
      ElMessage.success(t('assistant.created'))
    }).catch(() => {})
}

async function onSend() {
  if (!current.value || !input.value.trim()) return
  const text = input.value
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  await nextTick()
  scrollBottom()
  streaming.value = true
  streamingContent.value = ''

  const url = chatApi.streamUrl(current.value.id)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({ message: text })
    })
    if (!resp.body) throw new Error('No stream')
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            const evt: AssistantStreamEvent = JSON.parse(line.slice(5).trim())
            handleEvent(evt)
          } catch {}
        }
      }
    }
  } catch (e: any) {
    ElMessage.error(t('assistant.sendFailed', { msg: e?.message || '' }))
  } finally {
    streaming.value = false
    streamingContent.value = ''
    await nextTick()
    scrollBottom()
  }
}

function handleEvent(evt: AssistantStreamEvent) {
  if (evt.type === 'content') {
    streamingContent.value += evt.chunk || ''
  } else if (evt.type === 'tool_call') {
    messages.value.push({ role: 'tool', name: evt.name, args: JSON.stringify(evt.args) })
    scrollBottom()
  } else if (evt.type === 'tool_result') {
    messages.value.push({ role: 'tool_result', content: typeof evt.result === 'string' ? evt.result : JSON.stringify(evt.result) })
    scrollBottom()
  } else if (evt.type === 'done') {
    if (streamingContent.value) {
      messages.value.push({
        role: 'assistant',
        content: streamingContent.value,
        tokens: (evt.inputTokens || 0) + (evt.outputTokens || 0),
        costMs: evt.costMs
      })
    }
  }
}

function scrollBottom() {
  const el = document.querySelector('.as-main')
  if (el) el.scrollTop = el.scrollHeight
}

async function onAddEvent() {
  if (!current.value) return
  const { value } = await ElMessageBox.prompt(t('assistant.addEventTip'), t('assistant.addEventTitle'), { inputValue: t('assistant.addEventDefault') })
  await eventApi.create(current.value.id, { eventType: value, enabled: 1 } as any)
  events.value = await eventApi.list(current.value.id)
  ElMessage.success(t('assistant.added'))
}

async function onRemoveEvent(ev: AssistantEventVO) {
  await eventApi.remove(current.value!.id, ev.id)
  events.value = await eventApi.list(current.value!.id)
}

onMounted(load)
</script>

<style scoped>
.assistant-page { padding: 0; }
.as-container { height: calc(100vh - 56px - 32px); }
.as-aside { background: var(--ai-chat-aside-bg); border-right: 1px solid var(--ai-border); transition: width 0.2s; overflow: hidden; }
.as-list { padding: 12px; }
.new-btn { width: 100%; margin-bottom: 12px; }
.as-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 4px; cursor: pointer; margin-bottom: 4px; color: var(--ai-text); }
.as-item:hover { background: var(--ai-bg-hover); }
.as-item.active { background: var(--ai-primary); color: var(--ai-text-on-primary); }
.as-item .meta { flex: 1; min-width: 0; }
.as-item .name { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.as-item .desc { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; opacity: 0.8; }
.as-header { display: flex; align-items: center; gap: 12px; background: var(--ai-bg-elevated); border-bottom: 1px solid var(--ai-border); padding: 0 16px; height: 56px; color: var(--ai-text); }
.title { font-weight: 600; color: var(--ai-text); }
.flex-spacer { flex: 1; }
.as-main { background: var(--ai-chat-main-bg); padding: 16px 24px; overflow-y: auto; }
.empty { text-align: center; padding-top: 80px; color: var(--ai-text-secondary); }
.empty-icon { color: var(--ai-text-placeholder); }
.messages { display: flex; flex-direction: column; gap: 14px; max-width: 800px; margin: 0 auto; }
.msg { display: flex; gap: 12px; }
.msg.user { flex-direction: row-reverse; }
.msg .content { background: var(--ai-chat-bubble-bg); color: var(--ai-text); padding: 10px 14px; border-radius: 8px; box-shadow: var(--ai-shadow-sm); max-width: 75%; }
.msg.user .content { background: var(--ai-chat-bubble-user-bg); color: var(--ai-chat-bubble-user-text); }
.msg.tool .content { background: var(--ai-chat-tool-bg); border-left: 3px solid var(--ai-warning); }
.msg.tool_result .content { background: var(--ai-chat-tool-result-bg); border-left: 3px solid var(--ai-success); }
.msg .content pre { margin: 0; white-space: pre-wrap; word-wrap: break-word; font-family: inherit; font-size: 13px; }
.msg .meta { font-size: 11px; opacity: 0.6; margin-top: 4px; }
.msg.streaming .content { border: 1px solid var(--ai-primary); }
.as-footer { display: flex; gap: 8px; align-items: flex-end; background: var(--ai-bg-elevated); border-top: 1px solid var(--ai-border); padding: 12px 16px; }
.as-footer .el-textarea { flex: 1; }
.config-aside { background: var(--ai-bg-elevated); border-left: 1px solid var(--ai-border); overflow-y: auto; }
.config-pane { padding: 16px; }
.config-pane h4 { margin: 0 0 12px; color: var(--ai-text); }
.mt { margin-top: 16px; }
.m-1 { margin-right: 4px; margin-bottom: 4px; }
.event-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
</style>
